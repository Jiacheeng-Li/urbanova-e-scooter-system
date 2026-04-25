package com.lcyhz.urbanova.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.entity.AuditLogEntity;
import com.lcyhz.urbanova.entity.BookingConfirmationEntity;
import com.lcyhz.urbanova.entity.BookingEntity;
import com.lcyhz.urbanova.entity.BookingEventEntity;
import com.lcyhz.urbanova.entity.NotificationEntity;
import com.lcyhz.urbanova.mapper.AuditLogMapper;
import com.lcyhz.urbanova.mapper.BookingConfirmationMapper;
import com.lcyhz.urbanova.mapper.BookingEventMapper;
import com.lcyhz.urbanova.mapper.NotificationMapper;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PlatformSupportService {
    private final BookingEventMapper bookingEventMapper;
    private final NotificationMapper notificationMapper;
    private final BookingConfirmationMapper bookingConfirmationMapper;
    private final AuditLogMapper auditLogMapper;

    public PlatformSupportService(BookingEventMapper bookingEventMapper,
                                  NotificationMapper notificationMapper,
                                  BookingConfirmationMapper bookingConfirmationMapper,
                                  AuditLogMapper auditLogMapper) {
        this.bookingEventMapper = bookingEventMapper;
        this.notificationMapper = notificationMapper;
        this.bookingConfirmationMapper = bookingConfirmationMapper;
        this.auditLogMapper = auditLogMapper;
    }

    public BookingEventEntity recordBookingEvent(String bookingId, String eventType, String actorUserId, String actorRole, String details) {
        BookingEventEntity entity = new BookingEventEntity();
        entity.setEventId(newPrefixedId("EVT", 10));
        entity.setBookingId(bookingId);
        entity.setEventType(eventType);
        entity.setActorUserId(actorUserId);
        entity.setActorRole(actorRole);
        entity.setDetails(trimToNull(details));
        entity.setCreatedAt(LocalDateTime.now());
        bookingEventMapper.insert(entity);
        return entity;
    }

    public NotificationEntity createNotification(String userId, String type, String title, String message, String relatedBookingId) {
        if (!hasText(userId)) {
            return null;
        }
        NotificationEntity entity = new NotificationEntity();
        entity.setNotificationId(newPrefixedId("NTF", 10));
        entity.setUserId(userId);
        entity.setType(type);
        entity.setTitle(limit(title, 120));
        entity.setMessage(limit(message, 255));
        entity.setReadFlag(0);
        entity.setRelatedBookingId(trimToNull(relatedBookingId));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        notificationMapper.insert(entity);
        return entity;
    }

    public BookingConfirmationEntity createOrRefreshConfirmation(BookingEntity booking,
                                                                 String recipientEmail,
                                                                 String channel,
                                                                 String message,
                                                                 boolean resend) {
        BookingConfirmationEntity existing = bookingConfirmationMapper.selectOne(new LambdaQueryWrapper<BookingConfirmationEntity>()
                .eq(BookingConfirmationEntity::getBookingId, booking.getBookingId())
                .orderByDesc(BookingConfirmationEntity::getUpdatedAt)
                .last("LIMIT 1"));

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            BookingConfirmationEntity entity = new BookingConfirmationEntity();
            entity.setConfirmationId(newPrefixedId("CNF", 10));
            entity.setBookingId(booking.getBookingId());
            entity.setUserId(booking.getUserId());
            entity.setRecipientEmail(trimToNull(recipientEmail));
            entity.setChannel(channel);
            entity.setStatus(resend ? "RESENT" : "SENT");
            entity.setMessage(limit(message, 255));
            entity.setResendCount(resend ? 1 : 0);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            bookingConfirmationMapper.insert(entity);
            return entity;
        }

        existing.setRecipientEmail(trimToNull(recipientEmail));
        existing.setChannel(channel);
        existing.setStatus(resend ? "RESENT" : "SENT");
        existing.setMessage(limit(message, 255));
        existing.setResendCount((existing.getResendCount() == null ? 0 : existing.getResendCount()) + (resend ? 1 : 0));
        existing.setUpdatedAt(now);
        bookingConfirmationMapper.updateById(existing);
        return existing;
    }

    public void recordAudit(String action, String targetType, String targetId, String details) {
        AuthUser authUser = AuthContext.getCurrentUser();
        recordAudit(authUser == null ? null : authUser.getUserId(),
                authUser == null ? "SYSTEM" : authUser.getRole(),
                action,
                targetType,
                targetId,
                details);
    }

    public void recordAudit(String actorUserId, String actorRole, String action, String targetType, String targetId, String details) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setAuditLogId(newPrefixedId("AUD", 10));
        entity.setActorUserId(trimToNull(actorUserId));
        entity.setActorRole(hasText(actorRole) ? actorRole : "SYSTEM");
        entity.setAction(action);
        entity.setTargetType(targetType);
        entity.setTargetId(trimToNull(targetId));
        entity.setDetails(limit(details, 255));
        entity.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(entity);
    }

    public List<Map<String, Object>> listBookingTimeline(String bookingId) {
        return bookingEventMapper.selectList(new LambdaQueryWrapper<BookingEventEntity>()
                        .eq(BookingEventEntity::getBookingId, bookingId)
                        .orderByAsc(BookingEventEntity::getCreatedAt))
                .stream()
                .map(this::toBookingEventMap)
                .toList();
    }

    public List<Map<String, Object>> listNotifications(String userId) {
        return notificationMapper.selectList(new LambdaQueryWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getUserId, userId)
                        .orderByDesc(NotificationEntity::getCreatedAt))
                .stream()
                .map(this::toNotificationMap)
                .toList();
    }

    public List<Map<String, Object>> listAuditLogs(String action, Integer limit) {
        LambdaQueryWrapper<AuditLogEntity> query = new LambdaQueryWrapper<AuditLogEntity>()
                .orderByDesc(AuditLogEntity::getCreatedAt);
        if (hasText(action)) {
            query.eq(AuditLogEntity::getAction, action.trim().toUpperCase(Locale.ROOT));
        }
        if (limit != null && limit > 0) {
            query.last("LIMIT " + Math.min(limit, 200));
        }
        return auditLogMapper.selectList(query).stream().map(this::toAuditLogMap).toList();
    }

    public Map<String, Object> toBookingEventMap(BookingEventEntity entity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("eventId", entity.getEventId());
        data.put("bookingId", entity.getBookingId());
        data.put("eventType", entity.getEventType());
        data.put("actorUserId", entity.getActorUserId());
        data.put("actorRole", entity.getActorRole());
        data.put("details", entity.getDetails());
        data.put("createdAt", entity.getCreatedAt());
        return data;
    }

    public Map<String, Object> toNotificationMap(NotificationEntity entity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("notificationId", entity.getNotificationId());
        data.put("type", entity.getType());
        data.put("title", entity.getTitle());
        data.put("message", entity.getMessage());
        data.put("read", entity.getReadFlag() != null && entity.getReadFlag() == 1);
        data.put("relatedBookingId", entity.getRelatedBookingId());
        data.put("createdAt", entity.getCreatedAt());
        data.put("updatedAt", entity.getUpdatedAt());
        return data;
    }

    public Map<String, Object> toConfirmationMap(BookingConfirmationEntity entity) {
        if (entity == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Confirmation not found");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("confirmationId", entity.getConfirmationId());
        data.put("bookingId", entity.getBookingId());
        data.put("userId", entity.getUserId());
        data.put("recipientEmail", entity.getRecipientEmail());
        data.put("channel", entity.getChannel());
        data.put("status", entity.getStatus());
        data.put("message", entity.getMessage());
        data.put("resendCount", entity.getResendCount());
        data.put("createdAt", entity.getCreatedAt());
        data.put("updatedAt", entity.getUpdatedAt());
        return data;
    }

    public Map<String, Object> toAuditLogMap(AuditLogEntity entity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("auditLogId", entity.getAuditLogId());
        data.put("actorUserId", entity.getActorUserId());
        data.put("actorRole", entity.getActorRole());
        data.put("action", entity.getAction());
        data.put("targetType", entity.getTargetType());
        data.put("targetId", entity.getTargetId());
        data.put("details", entity.getDetails());
        data.put("createdAt", entity.getCreatedAt());
        return data;
    }

    public String requireText(Object value, String fieldName) {
        if (value == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, fieldName + " is required");
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, fieldName + " is required");
        }
        return text;
    }

    public String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public String newPrefixedId(String prefix, int len) {
        String raw = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return prefix + "-" + raw.substring(0, len);
    }

    private String limit(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
