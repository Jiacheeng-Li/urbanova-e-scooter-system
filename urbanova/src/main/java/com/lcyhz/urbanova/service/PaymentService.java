package com.lcyhz.urbanova.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.entity.BookingEntity;
import com.lcyhz.urbanova.entity.PaymentEntity;
import com.lcyhz.urbanova.entity.PaymentMethodEntity;
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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentMapper paymentMapper;
    private final BookingMapper bookingMapper;
    private final PaymentMethodService paymentMethodService;
    private final PlatformSupportService platformSupportService;
    private final UserMapper userMapper;

    public PaymentService(PaymentMapper paymentMapper,
                          BookingMapper bookingMapper,
                          PaymentMethodService paymentMethodService,
                          PlatformSupportService platformSupportService,
                          UserMapper userMapper) {
        this.paymentMapper = paymentMapper;
        this.bookingMapper = bookingMapper;
        this.paymentMethodService = paymentMethodService;
        this.platformSupportService = platformSupportService;
        this.userMapper = userMapper;
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public Map<String, Object> createPayment(String userId, String role, String bookingId, Map<String, Object> request) {
        BookingEntity booking = requireAccessibleBooking(userId, role, bookingId);
        if (DomainConstants.BookingStatus.CANCELLED.equals(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.BOOKING_CONFLICT, "Cancelled bookings cannot be paid");
        }

        String method = normalizeMethod(stringValue(request == null ? null : request.get("method")));
        String paymentMethodId = null;
        if (DomainConstants.PaymentMethodType.SAVED_CARD.equals(method)) {
            paymentMethodId = requireText(stringValue(request == null ? null : request.get("paymentMethodId")), "paymentMethodId");
            PaymentMethodEntity paymentMethod = paymentMethodService.requireActiveOwnedMethod(userId, paymentMethodId);
            paymentMethodId = paymentMethod.getPaymentMethodId();
        }

        BigDecimal outstanding = calculateOutstandingAmount(booking.getBookingId(), booking.getPriceFinal());
        BigDecimal amount = request != null && request.get("amount") != null
                ? parseBigDecimal(request.get("amount"))
                : outstanding;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "amount must be greater than zero");
        }
        if (amount.compareTo(outstanding) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "amount exceeds outstanding balance");
        }

        PaymentEntity payment = new PaymentEntity();
        payment.setPaymentId("PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
        payment.setBookingId(booking.getBookingId());
        payment.setUserId(booking.getUserId());
        payment.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        payment.setMethod(method);
        payment.setPaymentMethodId(paymentMethodId);
        payment.setStatus(DomainConstants.PaymentStatus.INITIATED);
        payment.setRefundedAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.insert(payment);

        boolean deferSettlement = parseBoolean(request == null ? null : request.get("deferSettlement"));
        String simulatedOutcome = normalizeOutcome(stringValue(request == null ? null : request.get("simulatedOutcome")));
        if (!deferSettlement) {
            applySettlement(payment, booking, simulatedOutcome, role, userId);
        }
        return toPaymentMap(paymentMapper.selectOne(new LambdaQueryWrapper<PaymentEntity>()
                .eq(PaymentEntity::getPaymentId, payment.getPaymentId())));
    }

    public List<Map<String, Object>> listBookingPayments(String userId, String role, String bookingId) {
        requireAccessibleBooking(userId, role, bookingId);
        return paymentMapper.selectList(new LambdaQueryWrapper<PaymentEntity>()
                        .eq(PaymentEntity::getBookingId, bookingId)
                        .orderByDesc(PaymentEntity::getCreatedAt))
                .stream()
                .map(this::toPaymentMap)
                .toList();
    }

    public Map<String, Object> getPaymentDetail(String userId, String role, String paymentId) {
        PaymentEntity payment = requirePayment(paymentId);
        requireAccessibleBooking(userId, role, payment.getBookingId());
        return toPaymentMap(payment);
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public Map<String, Object> simulateSettlement(String paymentId, String outcome, String actorUserId, String actorRole) {
        PaymentEntity payment = requirePayment(paymentId);
        if (!DomainConstants.PaymentStatus.INITIATED.equals(payment.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.PAYMENT_FAILED, "Payment is not awaiting settlement");
        }
        BookingEntity booking = requireBooking(payment.getBookingId());
        applySettlement(payment, booking, normalizeOutcome(outcome), actorRole, actorUserId);
        return toPaymentMap(paymentMapper.selectOne(new LambdaQueryWrapper<PaymentEntity>()
                .eq(PaymentEntity::getPaymentId, paymentId)));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> refundPayment(String paymentId, Map<String, Object> request) {
        PaymentEntity payment = requirePayment(paymentId);
        if (!DomainConstants.PaymentStatus.SUCCEEDED.equals(payment.getStatus())
                && !DomainConstants.PaymentStatus.REFUNDED.equals(payment.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.PAYMENT_FAILED, "Only succeeded payments can be refunded");
        }

        BigDecimal refundable = payment.getAmount().subtract(zeroIfNull(payment.getRefundedAmount()));
        BigDecimal refundAmount = request != null && request.get("amount") != null
                ? parseBigDecimal(request.get("amount"))
                : refundable;
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0 || refundAmount.compareTo(refundable) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "Invalid refund amount");
        }

        payment.setRefundedAmount(zeroIfNull(payment.getRefundedAmount()).add(refundAmount).setScale(2, RoundingMode.HALF_UP));
        if (payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(DomainConstants.PaymentStatus.REFUNDED);
        }
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        BookingEntity booking = requireBooking(payment.getBookingId());
        BigDecimal outstanding = calculateOutstandingAmount(booking.getBookingId(), booking.getPriceFinal());
        if (outstanding.compareTo(booking.getPriceFinal()) >= 0) {
            booking.setPaymentStatus(DomainConstants.PAYMENT_STATUS_REFUNDED);
        } else if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
            booking.setPaymentStatus(DomainConstants.PAYMENT_STATUS_PARTIAL);
        } else {
            booking.setPaymentStatus(DomainConstants.PAYMENT_STATUS_PAID);
        }
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.updateById(booking);
        platformSupportService.recordBookingEvent(booking.getBookingId(), "PAYMENT_REFUNDED", null, DomainConstants.ROLE_MANAGER,
                "Refunded payment " + payment.getPaymentId());
        platformSupportService.recordAudit("PAYMENT_REFUNDED", "PAYMENT", payment.getPaymentId(), "refundAmount=" + refundAmount);
        platformSupportService.createNotification(booking.getUserId(), DomainConstants.NotificationType.PAYMENT_UPDATED,
                "Refund processed", "Refund recorded for booking " + booking.getBookingRef(), booking.getBookingId());
        return toPaymentMap(payment);
    }

    private void applySettlement(PaymentEntity payment, BookingEntity booking, String outcome, String actorRole, String actorUserId) {
        payment.setSimulatedOutcome(outcome);
        payment.setStatus(DomainConstants.PaymentOutcome.FAILURE.equals(outcome)
                ? DomainConstants.PaymentStatus.FAILED
                : DomainConstants.PaymentStatus.SUCCEEDED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        if (DomainConstants.PaymentStatus.FAILED.equals(payment.getStatus())) {
            platformSupportService.recordBookingEvent(booking.getBookingId(), "PAYMENT_FAILED", actorUserId, actorRole,
                    "Payment " + payment.getPaymentId() + " failed");
            platformSupportService.createNotification(booking.getUserId(), DomainConstants.NotificationType.PAYMENT_UPDATED,
                    "Payment failed", "Payment failed for booking " + booking.getBookingRef(), booking.getBookingId());
            throw new BusinessException(HttpStatus.PAYMENT_REQUIRED.value(), ErrorCodes.PAYMENT_FAILED, "Simulated payment failed");
        }

        BigDecimal outstanding = calculateOutstandingAmount(booking.getBookingId(), booking.getPriceFinal());
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            booking.setPaymentStatus(DomainConstants.PAYMENT_STATUS_PAID);
            if (DomainConstants.BookingStatus.PENDING_PAYMENT.equals(booking.getStatus())) {
                booking.setStatus(DomainConstants.BookingStatus.CONFIRMED);
            }
        } else {
            booking.setPaymentStatus(DomainConstants.PAYMENT_STATUS_PARTIAL);
        }
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.updateById(booking);

        platformSupportService.recordBookingEvent(booking.getBookingId(), "PAYMENT_SUCCEEDED", actorUserId, actorRole,
                "Payment " + payment.getPaymentId() + " succeeded");
        platformSupportService.createNotification(booking.getUserId(), DomainConstants.NotificationType.PAYMENT_UPDATED,
                "Payment received", "Payment recorded for booking " + booking.getBookingRef(), booking.getBookingId());

        if (DomainConstants.PAYMENT_STATUS_PAID.equals(booking.getPaymentStatus())) {
            String recipientEmail = resolveRecipientEmail(booking);
            platformSupportService.createOrRefreshConfirmation(
                    booking,
                    recipientEmail,
                    "EMAIL",
                    "Booking confirmation for " + booking.getBookingRef(),
                    false);
            platformSupportService.createNotification(booking.getUserId(), DomainConstants.NotificationType.BOOKING_CONFIRMATION,
                    "Booking confirmed", "Booking " + booking.getBookingRef() + " is confirmed", booking.getBookingId());
        }
    }

    private BookingEntity requireAccessibleBooking(String userId, String role, String bookingId) {
        BookingEntity booking = requireBooking(bookingId);
        if (DomainConstants.ROLE_MANAGER.equals(role) || DomainConstants.ROLE_STAFF.equals(role)) {
            return booking;
        }
        if (booking.getUserId() == null || !booking.getUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), ErrorCodes.AUTH_FORBIDDEN, "No permission for this booking");
        }
        return booking;
    }

    private BookingEntity requireBooking(String bookingId) {
        BookingEntity booking = bookingMapper.selectOne(new LambdaQueryWrapper<BookingEntity>()
                .eq(BookingEntity::getBookingId, bookingId));
        if (booking == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Booking not found");
        }
        return booking;
    }

    private PaymentEntity requirePayment(String paymentId) {
        PaymentEntity payment = paymentMapper.selectOne(new LambdaQueryWrapper<PaymentEntity>()
                .eq(PaymentEntity::getPaymentId, paymentId));
        if (payment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Payment not found");
        }
        return payment;
    }

    private BigDecimal calculateOutstandingAmount(String bookingId, BigDecimal bookingTotal) {
        List<PaymentEntity> payments = paymentMapper.selectList(new LambdaQueryWrapper<PaymentEntity>()
                .eq(PaymentEntity::getBookingId, bookingId));
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        for (PaymentEntity payment : payments) {
            if (DomainConstants.PaymentStatus.SUCCEEDED.equals(payment.getStatus())
                    || DomainConstants.PaymentStatus.REFUNDED.equals(payment.getStatus())) {
                paid = paid.add(zeroIfNull(payment.getAmount()));
                refunded = refunded.add(zeroIfNull(payment.getRefundedAmount()));
            }
        }
        return bookingTotal.subtract(paid.subtract(refunded)).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveRecipientEmail(BookingEntity booking) {
        if (booking.getUserId() != null) {
            UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                    .eq(UserEntity::getUserId, booking.getUserId()));
            if (user != null) {
                return user.getEmail();
            }
        }
        return booking.getGuestEmail();
    }

    private String normalizeMethod(String value) {
        if (value == null || value.isBlank()) {
            return DomainConstants.PaymentMethodType.ONE_TIME_CARD;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!List.of(DomainConstants.PaymentMethodType.SAVED_CARD, DomainConstants.PaymentMethodType.ONE_TIME_CARD).contains(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "method must be SAVED_CARD or ONE_TIME_CARD");
        }
        return normalized;
    }

    private String normalizeOutcome(String value) {
        if (value == null || value.isBlank()) {
            return DomainConstants.PaymentOutcome.SUCCESS;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!List.of(DomainConstants.PaymentOutcome.SUCCESS, DomainConstants.PaymentOutcome.FAILURE).contains(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "simulatedOutcome must be SUCCESS or FAILURE");
        }
        return normalized;
    }

    private String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " is required");
        }
        return value.trim();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean parseBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private BigDecimal parseBigDecimal(Object value) {
        try {
            return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "Invalid decimal value: " + value);
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Map<String, Object> toPaymentMap(PaymentEntity entity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentId", entity.getPaymentId());
        data.put("bookingId", entity.getBookingId());
        data.put("userId", entity.getUserId());
        data.put("amount", entity.getAmount());
        data.put("method", entity.getMethod());
        data.put("paymentMethodId", entity.getPaymentMethodId());
        data.put("status", entity.getStatus());
        data.put("simulatedOutcome", entity.getSimulatedOutcome());
        data.put("refundedAmount", entity.getRefundedAmount());
        data.put("createdAt", entity.getCreatedAt());
        data.put("updatedAt", entity.getUpdatedAt());
        return data;
    }
}
