package com.lcyhz.urbanova.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.entity.IssueCommentEntity;
import com.lcyhz.urbanova.entity.IssueEntity;
import com.lcyhz.urbanova.mapper.IssueCommentMapper;
import com.lcyhz.urbanova.mapper.IssueMapper;
import com.lcyhz.urbanova.service.support.PlatformSupportService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class IssueManagementService {
    private final IssueMapper issueMapper;
    private final IssueCommentMapper issueCommentMapper;
    private final PlatformSupportService platformSupportService;

    public IssueManagementService(IssueMapper issueMapper,
                                  IssueCommentMapper issueCommentMapper,
                                  PlatformSupportService platformSupportService) {
        this.issueMapper = issueMapper;
        this.issueCommentMapper = issueCommentMapper;
        this.platformSupportService = platformSupportService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createIssue(String userId, Map<String, Object> request) {
        IssueEntity issue = new IssueEntity();
        issue.setIssueId("ISS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
        issue.setReporterUserId(userId);
        issue.setBookingId(trimToNull(stringValue(request.get("bookingId"))));
        issue.setScooterId(trimToNull(stringValue(request.get("scooterId"))));
        issue.setTitle(requireText(stringValue(request.get("title")), "title"));
        issue.setDescription(requireText(stringValue(request.get("description")), "description"));
        issue.setPriority(normalizePriority(stringValue(request.get("priority")), DomainConstants.IssuePriority.LOW));
        issue.setStatus(DomainConstants.IssueStatus.OPEN);
        issue.setCreatedAt(LocalDateTime.now());
        issue.setUpdatedAt(LocalDateTime.now());
        issueMapper.insert(issue);
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
        IssueEntity issue = requireIssue(issueId);
        if (!DomainConstants.ROLE_MANAGER.equals(role) && !issue.getReporterUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), ErrorCodes.AUTH_FORBIDDEN, "No permission for this issue");
        }
        return toIssueMap(issue, loadComments(issueId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addComment(String userId, String role, String issueId, String message) {
        IssueEntity issue = requireIssue(issueId);
        if (!DomainConstants.ROLE_MANAGER.equals(role) && !issue.getReporterUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), ErrorCodes.AUTH_FORBIDDEN, "No permission for this issue");
        }
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
        platformSupportService.recordAudit("ISSUE_RESOLVED", "ISSUE", issueId, feedback);
        platformSupportService.createNotification(issue.getReporterUserId(), DomainConstants.NotificationType.ISSUE_UPDATED,
                "Issue resolved", "Issue " + issue.getIssueId() + " was resolved", issue.getBookingId());
        return toIssueMap(issue, loadComments(issueId));
    }

    public List<Map<String, Object>> listHighPriorityIssues() {
        return issueMapper.selectList(new LambdaQueryWrapper<IssueEntity>()
                        .in(IssueEntity::getPriority, DomainConstants.IssuePriority.HIGH, DomainConstants.IssuePriority.CRITICAL)
                        .orderByDesc(IssueEntity::getUpdatedAt))
                .stream()
                .map(issue -> toIssueMap(issue, null))
                .toList();
    }

    private IssueEntity requireIssue(String issueId) {
        IssueEntity issue = issueMapper.selectOne(new LambdaQueryWrapper<IssueEntity>()
                .eq(IssueEntity::getIssueId, issueId));
        if (issue == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Issue not found");
        }
        return issue;
    }

    private List<Map<String, Object>> loadComments(String issueId) {
        return issueCommentMapper.selectList(new LambdaQueryWrapper<IssueCommentEntity>()
                        .eq(IssueCommentEntity::getIssueId, issueId)
                        .orderByAsc(IssueCommentEntity::getCreatedAt))
                .stream()
                .map(this::toCommentMap)
                .toList();
    }

    private Map<String, Object> toIssueMap(IssueEntity issue, List<Map<String, Object>> comments) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("issueId", issue.getIssueId());
        data.put("reporterUserId", issue.getReporterUserId());
        data.put("bookingId", issue.getBookingId());
        data.put("scooterId", issue.getScooterId());
        data.put("title", issue.getTitle());
        data.put("description", issue.getDescription());
        data.put("priority", issue.getPriority());
        data.put("status", issue.getStatus());
        data.put("managerFeedback", issue.getManagerFeedback());
        data.put("createdAt", issue.getCreatedAt());
        data.put("updatedAt", issue.getUpdatedAt());
        if (comments != null) {
            data.put("comments", comments);
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

    private String requireText(String value, String field) {
        if (!hasText(value)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " is required");
        }
        return value.trim();
    }

    private String normalizePriority(String value, String fallback) {
        String resolved = hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
        if (!Set.of(DomainConstants.IssuePriority.LOW, DomainConstants.IssuePriority.HIGH, DomainConstants.IssuePriority.CRITICAL).contains(resolved)) {
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
}
