package com.lcyhz.urbanova.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.entity.BookingEntity;
import com.lcyhz.urbanova.entity.PaymentEntity;
import com.lcyhz.urbanova.entity.UserEntity;
import com.lcyhz.urbanova.mapper.BookingMapper;
import com.lcyhz.urbanova.mapper.PaymentMapper;
import com.lcyhz.urbanova.mapper.UserMapper;
import com.lcyhz.urbanova.service.support.PlatformSupportService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserManagementService {
    private final UserMapper userMapper;
    private final BookingMapper bookingMapper;
    private final PaymentMapper paymentMapper;
    private final PlatformSupportService platformSupportService;
    private final DiscountRuleService discountRuleService;

    public UserManagementService(UserMapper userMapper,
                                 BookingMapper bookingMapper,
                                 PaymentMapper paymentMapper,
                                 PlatformSupportService platformSupportService,
                                 DiscountRuleService discountRuleService) {
        this.userMapper = userMapper;
        this.bookingMapper = bookingMapper;
        this.paymentMapper = paymentMapper;
        this.platformSupportService = platformSupportService;
        this.discountRuleService = discountRuleService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateCurrentUser(String userId, Map<String, Object> request) {
        UserEntity user = requireUser(userId);
        if (request.containsKey("fullName")) {
            user.setFullName(requireText(request.get("fullName"), "fullName"));
        }
        if (request.containsKey("phone")) {
            user.setPhone(trimToNull((String) request.get("phone")));
        }
        if (request.containsKey("discountCategory")) {
            user.setDiscountCategory(normalizeDiscountCategory((String) request.get("discountCategory")));
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toUserMap(user);
    }

    public Map<String, Object> getUsageSummary(String userId) {
        requireUser(userId);
        List<BookingEntity> bookings = bookingMapper.selectList(new LambdaQueryWrapper<BookingEntity>()
                .eq(BookingEntity::getUserId, userId));
        List<PaymentEntity> payments = paymentMapper.selectList(new LambdaQueryWrapper<PaymentEntity>()
                .eq(PaymentEntity::getUserId, userId)
                .eq(PaymentEntity::getStatus, DomainConstants.PaymentStatus.SUCCEEDED));

        long totalMinutes = 0L;
        for (BookingEntity booking : bookings) {
            LocalDateTime start = booking.getActualStartAt() != null ? booking.getActualStartAt() : booking.getStartAt();
            LocalDateTime end = booking.getActualEndAt() != null ? booking.getActualEndAt() : booking.getEndAt();
            if (start != null && end != null && !end.isBefore(start)
                    && !DomainConstants.BookingStatus.CANCELLED.equals(booking.getStatus())) {
                totalMinutes += Duration.between(start, end).toMinutes();
            }
        }

        BigDecimal totalSpent = payments.stream()
                .map(PaymentEntity::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("bookingCount", bookings.size());
        data.put("hoursUsed", BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        data.put("totalSpent", totalSpent.setScale(2, RoundingMode.HALF_UP));
        data.put("hoursLast7Days", discountRuleService.calculateRecentHours(userId));
        data.put("discountEligibility", discountRuleService.getEligibility(userId));
        return data;
    }

    public List<Map<String, Object>> listUsers(String role, String accountStatus) {
        LambdaQueryWrapper<UserEntity> query = new LambdaQueryWrapper<UserEntity>()
                .orderByAsc(UserEntity::getRole)
                .orderByAsc(UserEntity::getEmail);
        if (hasText(role)) {
            query.eq(UserEntity::getRole, role.trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(accountStatus)) {
            query.eq(UserEntity::getAccountStatus, accountStatus.trim().toUpperCase(Locale.ROOT));
        }
        return userMapper.selectList(query).stream().map(this::toUserMap).toList();
    }

    public Map<String, Object> getUserDetail(String userId) {
        return toUserMap(requireUser(userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateUserStatus(String userId, String accountStatus) {
        UserEntity user = requireUser(userId);
        String normalized = normalizeAccountStatus(accountStatus);
        user.setAccountStatus(normalized);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        platformSupportService.recordAudit("USER_STATUS_UPDATED", "USER", userId, "accountStatus=" + normalized);
        return toUserMap(user);
    }

    public List<Map<String, Object>> listUserBookings(String userId) {
        requireUser(userId);
        return bookingMapper.selectList(new LambdaQueryWrapper<BookingEntity>()
                        .eq(BookingEntity::getUserId, userId)
                        .orderByDesc(BookingEntity::getCreatedAt))
                .stream()
                .map(this::toBookingMap)
                .toList();
    }

    public List<Map<String, Object>> listAuditLogs(String action, Integer limit) {
        return platformSupportService.listAuditLogs(action, limit);
    }

    public Map<String, Object> toUserMap(UserEntity user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getUserId());
        data.put("email", user.getEmail());
        data.put("fullName", user.getFullName());
        data.put("phone", user.getPhone());
        data.put("role", user.getRole());
        data.put("discountCategory", user.getDiscountCategory());
        data.put("accountStatus", user.getAccountStatus());
        data.put("createdAt", user.getCreatedAt());
        data.put("updatedAt", user.getUpdatedAt());
        return data;
    }

    public Map<String, Object> toBookingMap(BookingEntity booking) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bookingId", booking.getBookingId());
        data.put("bookingRef", booking.getBookingRef());
        data.put("customerType", booking.getCustomerType());
        data.put("userId", booking.getUserId());
        data.put("guestName", booking.getGuestName());
        data.put("guestEmail", booking.getGuestEmail());
        data.put("guestPhone", booking.getGuestPhone());
        data.put("scooterId", booking.getScooterId());
        data.put("hireOptionId", booking.getHireOptionId());
        data.put("startAt", booking.getStartAt());
        data.put("endAt", booking.getEndAt());
        data.put("actualStartAt", booking.getActualStartAt());
        data.put("actualEndAt", booking.getActualEndAt());
        data.put("status", booking.getStatus());
        data.put("priceBase", booking.getPriceBase());
        data.put("priceDiscount", booking.getPriceDiscount());
        data.put("priceFinal", booking.getPriceFinal());
        data.put("paymentStatus", booking.getPaymentStatus());
        data.put("createdByRole", booking.getCreatedByRole());
        data.put("cancelReason", booking.getCancelReason());
        data.put("createdAt", booking.getCreatedAt());
        data.put("updatedAt", booking.getUpdatedAt());
        return data;
    }

    public UserEntity requireUser(String userId) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId, userId));
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
        }
        return user;
    }

    private String requireText(Object value, String field) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " is required");
        }
        return String.valueOf(value).trim();
    }

    private String normalizeDiscountCategory(String value) {
        if (!hasText(value)) {
            return DomainConstants.DISCOUNT_NONE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!List.of(DomainConstants.DISCOUNT_NONE, DomainConstants.DISCOUNT_STUDENT, DomainConstants.DISCOUNT_SENIOR).contains(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "discountCategory must be NONE, STUDENT, or SENIOR");
        }
        return normalized;
    }

    private String normalizeAccountStatus(String value) {
        String normalized = requireText(value, "accountStatus").toUpperCase(Locale.ROOT);
        if (!List.of(DomainConstants.ACCOUNT_ACTIVE, DomainConstants.ACCOUNT_SUSPENDED, DomainConstants.ACCOUNT_DELETED).contains(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "accountStatus must be ACTIVE, SUSPENDED, or DELETED");
        }
        return normalized;
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
