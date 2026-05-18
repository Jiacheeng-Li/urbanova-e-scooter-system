package com.lcyhz.urbanova.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.entity.IssueCommentEntity;
import com.lcyhz.urbanova.entity.IssueEntity;
import com.lcyhz.urbanova.entity.IssuePhotoEntity;
import com.lcyhz.urbanova.entity.ScooterEntity;
import com.lcyhz.urbanova.entity.UserEntity;
import com.lcyhz.urbanova.mapper.IssueCommentMapper;
import com.lcyhz.urbanova.mapper.IssueMapper;
import com.lcyhz.urbanova.mapper.IssuePhotoMapper;
import com.lcyhz.urbanova.mapper.ScooterMapper;
import com.lcyhz.urbanova.mapper.UserMapper;
import com.lcyhz.urbanova.service.support.EmailDeliveryService;
import com.lcyhz.urbanova.service.support.PlatformSupportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class IssueManagementService {
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final IssueMapper issueMapper;
    private final IssueCommentMapper issueCommentMapper;
    private final IssuePhotoMapper issuePhotoMapper;
    private final ScooterMapper scooterMapper;
    private final UserMapper userMapper;
    private final EmailDeliveryService emailDeliveryService;
    private final PlatformSupportService platformSupportService;

    @Value("${app.storage.issue-photos-dir:storage/issues}")
    private String issuePhotoStorageDir;

    @Value("${app.scooter.low-battery-threshold:20}")
    private int lowBatteryThreshold;

    public IssueManagementService(IssueMapper issueMapper,
                                  IssueCommentMapper issueCommentMapper,
                                  IssuePhotoMapper issuePhotoMapper,
                                  ScooterMapper scooterMapper,
                                  UserMapper userMapper,
                                  EmailDeliveryService emailDeliveryService,
                                  PlatformSupportService platformSupportService) {
        this.issueMapper = issueMapper;
        this.issueCommentMapper = issueCommentMapper;
        this.issuePhotoMapper = issuePhotoMapper;
        this.scooterMapper = scooterMapper;
        this.userMapper = userMapper;
        this.emailDeliveryService = emailDeliveryService;
        this.platformSupportService = platformSupportService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createIssue(String userId, Map<String, Object> request) {
        Map<String, Object> safeRequest = request == null ? Map.of() : request;
        String issueType = normalizeIssueType(stringValue(safeRequest.get("issueType")));
        IssueEntity issue = new IssueEntity();
        issue.setIssueId("ISS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
        issue.setReporterUserId(userId);
        issue.setBookingId(trimToNull(stringValue(safeRequest.get("bookingId"))));
        issue.setScooterId(trimToNull(stringValue(safeRequest.get("scooterId"))));
        issue.setIssueType(issueType);
        issue.setTitle(requireText(stringValue(safeRequest.get("title")), "title"));
        issue.setDescription(requireText(stringValue(safeRequest.get("description")), "description"));
        issue.setPriority(priorityForIssueType(issueType));
        issue.setStatus(DomainConstants.IssueStatus.OPEN);
        issue.setCreatedAt(LocalDateTime.now());
        issue.setUpdatedAt(LocalDateTime.now());
        issueMapper.insert(issue);
        markScooterFaultIfNeeded(issueType, issue.getScooterId());
        emailDeliveryService.sendIssueSubmissionEmail(
                findReporterEmail(userId),
                issue.getIssueId(),
                issue.getIssueType(),
                issue.getTitle(),
                issue.getPriority(),
                issue.getScooterId(),
                issue.getBookingId(),
                issue.getCreatedAt());
        return toIssueMap(issue, List.of());
    }

    public List<Map<String, Object>> listOwnIssues(String userId, String status) {
        LambdaQueryWrapper<IssueEntity> query = new LambdaQueryWrapper<IssueEntity>()
                .eq(IssueEntity::getReporterUserId, userId)
                .orderByDesc(IssueEntity::getUpdatedAt);
        if (hasText(status)) {
            query.eq(IssueEntity::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        return issueMapper.selectList(query).stream().map(issue -> toIssueMap(issue, null)).toList();
    }

    public Map<String, Object> getIssue(String userId, String role, String issueId) {
        IssueEntity issue = requireAccessibleIssue(userId, role, issueId);
        return toIssueMap(issue, loadComments(issueId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addComment(String userId, String role, String issueId, String message) {
        IssueEntity issue = requireAccessibleIssue(userId, role, issueId);
        IssueCommentEntity comment = new IssueCommentEntity();
        comment.setCommentId("COM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
        comment.setIssueId(issueId);
        comment.setAuthorUserId(userId);
        comment.setAuthorRole(role);
        comment.setMessage(requireText(message, "message"));
        comment.setCreatedAt(LocalDateTime.now());
        issueCommentMapper.insert(comment);
        issue.setUpdatedAt(LocalDateTime.now());
        issueMapper.updateById(issue);
        if (DomainConstants.ROLE_MANAGER.equals(role)) {
            platformSupportService.createNotification(issue.getReporterUserId(), DomainConstants.NotificationType.ISSUE_UPDATED,
                    "Issue updated", "Manager added a comment to issue " + issue.getIssueId(), issue.getBookingId());
        }
        return toIssueMap(issue, loadComments(issueId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addPhotos(String userId, String role, String issueId, List<MultipartFile> files) {
        IssueEntity issue = requireAccessibleIssue(userId, role, issueId);
        if (files == null || files.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.ISSUE_PHOTO_INVALID, "At least one image file is required");
        }
        if (files.size() > 5) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.ISSUE_PHOTO_INVALID, "A maximum of 5 images can be uploaded at one time");
        }

        Path issueDirectory = Paths.get(issuePhotoStorageDir, issueId).toAbsolutePath().normalize();
        try {
            Files.createDirectories(issueDirectory);
            for (MultipartFile file : files) {
                validatePhoto(file);
                String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
                String photoId = "IPH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
                Path filePath = issueDirectory.resolve(photoId + extension).normalize();
                Files.write(filePath, file.getBytes());

                IssuePhotoEntity entity = new IssuePhotoEntity();
                entity.setPhotoId(photoId);
                entity.setIssueId(issueId);
                entity.setOriginalFileName(file.getOriginalFilename());
                entity.setContentType(file.getContentType());
                entity.setFileSize(file.getSize());
                entity.setStoragePath(filePath.toString());
                entity.setCreatedAt(LocalDateTime.now());
                issuePhotoMapper.insert(entity);
            }
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorCodes.INTERNAL_ERROR,
                    "Failed to store issue photos");
        }

        issue.setUpdatedAt(LocalDateTime.now());
        issueMapper.updateById(issue);
        return toIssueMap(issue, loadComments(issueId));
    }

    public PhotoDownload loadPhoto(String userId, String role, String issueId, String photoId) {
        requireAccessibleIssue(userId, role, issueId);
        IssuePhotoEntity photo = issuePhotoMapper.selectOne(new LambdaQueryWrapper<IssuePhotoEntity>()
                .eq(IssuePhotoEntity::getIssueId, issueId)
                .eq(IssuePhotoEntity::getPhotoId, photoId));
        if (photo == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.ISSUE_PHOTO_NOT_FOUND, "Issue photo not found");
        }
        try {
            byte[] content = Files.readAllBytes(Paths.get(photo.getStoragePath()));
            return new PhotoDownload(photo.getOriginalFileName(), photo.getContentType(), new ByteArrayResource(content));
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorCodes.INTERNAL_ERROR,
                    "Failed to read issue photo");
        }
    }

    public List<Map<String, Object>> listAdminIssues(String status, String priority) {
        LambdaQueryWrapper<IssueEntity> query = new LambdaQueryWrapper<IssueEntity>()
                .orderByDesc(IssueEntity::getUpdatedAt);
        if (hasText(status)) {
            query.eq(IssueEntity::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(priority)) {
            query.eq(IssueEntity::getPriority, priority.trim().toUpperCase(Locale.ROOT));
        }
        return issueMapper.selectList(query).stream().map(issue -> toIssueMap(issue, null)).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updatePriority(String issueId, String priority) {
        IssueEntity issue = requireIssue(issueId);
        issue.setPriority(normalizePriority(priority, issue.getPriority()));
        issue.setUpdatedAt(LocalDateTime.now());
        issueMapper.updateById(issue);
        platformSupportService.recordAudit("ISSUE_PRIORITY_UPDATED", "ISSUE", issueId, "priority=" + issue.getPriority());
        platformSupportService.createNotification(issue.getReporterUserId(), DomainConstants.NotificationType.ISSUE_UPDATED,
                "Issue priority updated", "Priority changed for issue " + issue.getIssueId(), issue.getBookingId());
        return toIssueMap(issue, loadComments(issueId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateStatus(String issueId, String status) {
        IssueEntity issue = requireIssue(issueId);
        issue.setStatus(normalizeStatus(status));
        issue.setUpdatedAt(LocalDateTime.now());
        issueMapper.updateById(issue);
        if (Set.of(DomainConstants.IssueStatus.RESOLVED, DomainConstants.IssueStatus.CLOSED).contains(issue.getStatus())) {
            restoreScooterAvailability(issue.getScooterId());
        }
        platformSupportService.recordAudit("ISSUE_STATUS_UPDATED", "ISSUE", issueId, "status=" + issue.getStatus());
        platformSupportService.createNotification(issue.getReporterUserId(), DomainConstants.NotificationType.ISSUE_UPDATED,
                "Issue status updated", "Status changed for issue " + issue.getIssueId(), issue.getBookingId());
        return toIssueMap(issue, loadComments(issueId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> resolve(String issueId, String feedback) {
        IssueEntity issue = requireIssue(issueId);
        issue.setStatus(DomainConstants.IssueStatus.RESOLVED);
        issue.setManagerFeedback(trimToNull(feedback));
        issue.setUpdatedAt(LocalDateTime.now());
        issueMapper.updateById(issue);
        restoreScooterAvailability(issue.getScooterId());
        platformSupportService.recordAudit("ISSUE_RESOLVED", "ISSUE", issueId, feedback);
        platformSupportService.createNotification(issue.getReporterUserId(), DomainConstants.NotificationType.ISSUE_UPDATED,
                "Issue resolved", "Issue " + issue.getIssueId() + " was resolved", issue.getBookingId());
        return toIssueMap(issue, loadComments(issueId));
    }

    public List<Map<String, Object>> listHighPriorityIssues() {
        return issueMapper.selectList(new LambdaQueryWrapper<IssueEntity>()
                        .in(IssueEntity::getPriority,
                                DomainConstants.IssuePriority.HIGH,
                                DomainConstants.IssuePriority.URGENT,
                                DomainConstants.IssuePriority.CRITICAL)
                        .orderByDesc(IssueEntity::getUpdatedAt))
                .stream()
                .map(issue -> toIssueMap(issue, null))
                .toList();
    }

    private IssueEntity requireAccessibleIssue(String userId, String role, String issueId) {
        IssueEntity issue = requireIssue(issueId);
        if (!DomainConstants.ROLE_MANAGER.equals(role) && !issue.getReporterUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), ErrorCodes.AUTH_FORBIDDEN, "No permission for this issue");
        }
        return issue;
    }

    private IssueEntity requireIssue(String issueId) {
        IssueEntity issue = issueMapper.selectOne(new LambdaQueryWrapper<IssueEntity>()
                .eq(IssueEntity::getIssueId, issueId));
        if (issue == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Issue not found");
        }
        return issue;
    }

    private void markScooterFaultIfNeeded(String issueType, String scooterId) {
        if (!DomainConstants.IssueType.FAULT_REPORT.equals(issueType) || !hasText(scooterId)) {
            return;
        }
        ScooterEntity scooter = scooterMapper.selectOne(new LambdaQueryWrapper<ScooterEntity>()
                .eq(ScooterEntity::getScooterId, scooterId.trim().toUpperCase(Locale.ROOT)));
        if (scooter == null) {
            return;
        }
        if (Set.of(
                DomainConstants.ScooterStatus.IN_USE,
                DomainConstants.ScooterStatus.RESERVED,
                DomainConstants.ScooterStatus.CHARGING
        ).contains(scooter.getStatus())) {
            return;
        }
        UpdateWrapper<ScooterEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("scooter_id", scooter.getScooterId())
                .set("status", DomainConstants.ScooterStatus.FAULT)
                .set("updated_at", LocalDateTime.now())
                .setSql("version = version + 1");
        scooterMapper.update(null, updateWrapper);
    }

    private String findReporterEmail(String userId) {
        if (!hasText(userId)) {
            return null;
        }
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId, userId));
        return user == null ? null : trimToNull(user.getEmail());
    }

    private void restoreScooterAvailability(String scooterId) {
        if (!hasText(scooterId)) {
            return;
        }
        ScooterEntity scooter = scooterMapper.selectOne(new LambdaQueryWrapper<ScooterEntity>()
                .eq(ScooterEntity::getScooterId, scooterId.trim().toUpperCase(Locale.ROOT)));
        if (scooter == null) {
            return;
        }
        if (!Set.of(
                DomainConstants.ScooterStatus.FAULT,
                DomainConstants.ScooterStatus.UNDER_REPAIR,
                DomainConstants.ScooterStatus.MAINTENANCE,
                DomainConstants.ScooterStatus.UNAVAILABLE
        ).contains(scooter.getStatus())) {
            return;
        }
        String recoveredStatus = scooter.getBatteryPercent() != null && scooter.getBatteryPercent() < lowBatteryThreshold
                ? DomainConstants.ScooterStatus.LOW_BATTERY
                : DomainConstants.ScooterStatus.AVAILABLE;
        UpdateWrapper<ScooterEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("scooter_id", scooter.getScooterId())
                .set("status", recoveredStatus)
                .set("charge_started_at", null)
                .set("updated_at", LocalDateTime.now())
                .setSql("version = version + 1");
        scooterMapper.update(null, updateWrapper);
    }

    private List<Map<String, Object>> loadComments(String issueId) {
        return issueCommentMapper.selectList(new LambdaQueryWrapper<IssueCommentEntity>()
                        .eq(IssueCommentEntity::getIssueId, issueId)
                        .orderByAsc(IssueCommentEntity::getCreatedAt))
                .stream()
                .map(this::toCommentMap)
                .toList();
    }

    private List<Map<String, Object>> loadPhotos(String issueId) {
        return issuePhotoMapper.selectList(new LambdaQueryWrapper<IssuePhotoEntity>()
                        .eq(IssuePhotoEntity::getIssueId, issueId)
                        .orderByAsc(IssuePhotoEntity::getCreatedAt))
                .stream()
                .map(this::toPhotoMap)
                .toList();
    }

    private Map<String, Object> toIssueMap(IssueEntity issue, List<Map<String, Object>> comments) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("issueId", issue.getIssueId());
        data.put("reporterUserId", issue.getReporterUserId());
        data.put("bookingId", issue.getBookingId());
        data.put("scooterId", issue.getScooterId());
        data.put("issueType", issue.getIssueType());
        data.put("title", issue.getTitle());
        data.put("description", issue.getDescription());
        data.put("priority", issue.getPriority());
        data.put("status", issue.getStatus());
        data.put("managerFeedback", issue.getManagerFeedback());
        data.put("photoCount", issuePhotoMapper.selectCount(new LambdaQueryWrapper<IssuePhotoEntity>()
                .eq(IssuePhotoEntity::getIssueId, issue.getIssueId())));
        data.put("createdAt", issue.getCreatedAt());
        data.put("updatedAt", issue.getUpdatedAt());
        if (comments != null) {
            data.put("comments", comments);
            data.put("photos", loadPhotos(issue.getIssueId()));
        }
        return data;
    }

    private Map<String, Object> toCommentMap(IssueCommentEntity comment) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("commentId", comment.getCommentId());
        data.put("issueId", comment.getIssueId());
        data.put("authorUserId", comment.getAuthorUserId());
        data.put("authorRole", comment.getAuthorRole());
        data.put("message", comment.getMessage());
        data.put("createdAt", comment.getCreatedAt());
        return data;
    }

    private Map<String, Object> toPhotoMap(IssuePhotoEntity photo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("photoId", photo.getPhotoId());
        data.put("originalFileName", photo.getOriginalFileName());
        data.put("contentType", photo.getContentType());
        data.put("fileSize", photo.getFileSize());
        data.put("downloadPath", "/api/v1/issues/" + photo.getIssueId() + "/photos/" + photo.getPhotoId());
        data.put("createdAt", photo.getCreatedAt());
        return data;
    }

    private void validatePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.ISSUE_PHOTO_INVALID, "Uploaded image must not be empty");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(String.valueOf(file.getContentType()).toLowerCase(Locale.ROOT))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.ISSUE_PHOTO_INVALID,
                    "Only JPEG, PNG, and WEBP images are supported");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.ISSUE_PHOTO_INVALID,
                    "Each image must be 5 MB or smaller");
        }
    }

    private String resolveExtension(String originalFileName, String contentType) {
        if (originalFileName != null && originalFileName.contains(".")) {
            return originalFileName.substring(originalFileName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }

    private String requireText(String value, String field) {
        if (!hasText(value)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " is required");
        }
        return value.trim();
    }

    private String normalizeIssueType(String value) {
        String resolved = requireText(value, "issueType").toUpperCase(Locale.ROOT);
        if (!Set.of(DomainConstants.IssueType.FAULT_REPORT, DomainConstants.IssueType.COMPLAINT, DomainConstants.IssueType.OTHER).contains(resolved)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "issueType must be FAULT_REPORT, COMPLAINT, or OTHER");
        }
        return resolved;
    }

    private String priorityForIssueType(String issueType) {
        return switch (issueType) {
            case DomainConstants.IssueType.FAULT_REPORT -> DomainConstants.IssuePriority.URGENT;
            case DomainConstants.IssueType.COMPLAINT -> DomainConstants.IssuePriority.HIGH;
            default -> DomainConstants.IssuePriority.MEDIUM;
        };
    }

    private String normalizePriority(String value, String fallback) {
        String resolved = hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
        if (!Set.of(
                DomainConstants.IssuePriority.MEDIUM,
                DomainConstants.IssuePriority.HIGH,
                DomainConstants.IssuePriority.URGENT,
                DomainConstants.IssuePriority.LOW,
                DomainConstants.IssuePriority.CRITICAL
        ).contains(resolved)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "Invalid issue priority");
        }
        return resolved;
    }

    private String normalizeStatus(String value) {
        String resolved = requireText(value, "status").toUpperCase(Locale.ROOT);
        if (!Set.of(DomainConstants.IssueStatus.OPEN, DomainConstants.IssueStatus.IN_REVIEW, DomainConstants.IssueStatus.RESOLVED, DomainConstants.IssueStatus.CLOSED).contains(resolved)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "Invalid issue status");
        }
        return resolved;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public record PhotoDownload(String fileName, String contentType, ByteArrayResource resource) {
    }

    public List<Map<String, Object>> inRangeIssue(LocalDate startDate, LocalDate endDate) {
        // 设置默认日期范围（如果没有传入，默认最近30天）
        LocalDate resolvedStart = startDate == null ? LocalDate.now().minusDays(30) : startDate;
        LocalDate resolvedEnd = endDate == null ? LocalDate.now() : endDate;

        // 转换为 LocalDateTime 用于查询
        LocalDateTime startAt = resolvedStart.atStartOfDay();
        LocalDateTime endAt = resolvedEnd.plusDays(1).atStartOfDay();

        // 查询创建时间在指定范围内的所有 issue
        List<IssueEntity> issues = issueMapper.selectList(new LambdaQueryWrapper<IssueEntity>()
                .ge(IssueEntity::getCreatedAt, startAt)
                .lt(IssueEntity::getCreatedAt, endAt));

        // 使用常量初始化优先级统计 Map
        Map<String, Integer> priorityCount = new LinkedHashMap<>();
        priorityCount.put(DomainConstants.IssuePriority.CRITICAL, 0);
        priorityCount.put(DomainConstants.IssuePriority.URGENT, 0);
        priorityCount.put(DomainConstants.IssuePriority.HIGH, 0);
        priorityCount.put(DomainConstants.IssuePriority.MEDIUM, 0);
        priorityCount.put(DomainConstants.IssuePriority.LOW, 0);

        // 遍历结果集，统计每个优先级的数量
        for (IssueEntity issue : issues) {
            String priority = issue.getPriority();
            if (priority != null && priorityCount.containsKey(priority)) {
                priorityCount.put(priority, priorityCount.get(priority) + 1);
            }
        }

        // 构建适合前端饼图渲染的格式
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : priorityCount.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("priority", entry.getKey());
            item.put("label", entry.getKey());
            item.put("value", entry.getValue());
            result.add(item);
        }

        return result;
    }
}
