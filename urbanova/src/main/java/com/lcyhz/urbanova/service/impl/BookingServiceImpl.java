package com.lcyhz.urbanova.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.booking.CancelBookingRequest;
import com.lcyhz.urbanova.dto.booking.CreateBookingRequest;
import com.lcyhz.urbanova.dto.booking.UpdateBookingRequest;
import com.lcyhz.urbanova.entity.BookingConfirmationEntity;
import com.lcyhz.urbanova.entity.BookingEntity;
import com.lcyhz.urbanova.entity.HireOptionEntity;
import com.lcyhz.urbanova.entity.PaymentEntity;
import com.lcyhz.urbanova.entity.ScooterEntity;
import com.lcyhz.urbanova.mapper.BookingConfirmationMapper;
import com.lcyhz.urbanova.mapper.BookingMapper;
import com.lcyhz.urbanova.mapper.HireOptionMapper;
import com.lcyhz.urbanova.mapper.PaymentMapper;
import com.lcyhz.urbanova.mapper.ScooterMapper;
import com.lcyhz.urbanova.service.BookingService;
import com.lcyhz.urbanova.service.DiscountRuleService;
import com.lcyhz.urbanova.service.ScooterService;
import com.lcyhz.urbanova.service.support.PlatformSupportService;
import com.lcyhz.urbanova.vo.booking.BookingDetailVo;
import com.lcyhz.urbanova.vo.booking.BookingListItemVo;
import com.lcyhz.urbanova.vo.booking.CancelBookingVo;
import com.lcyhz.urbanova.vo.booking.CreateBookingVo;
import com.lcyhz.urbanova.vo.booking.PriceBreakdownVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private static final Set<String> CUSTOMER_MUTABLE_STATUSES = Set.of(
            DomainConstants.BookingStatus.PENDING_PAYMENT,
            DomainConstants.BookingStatus.CONFIRMED
    );

    private final BookingMapper bookingMapper;
    private final HireOptionMapper hireOptionMapper;
    private final ScooterMapper scooterMapper;
    private final PaymentMapper paymentMapper;
    private final BookingConfirmationMapper bookingConfirmationMapper;
    private final DiscountRuleService discountRuleService;
    private final ScooterService scooterService;
    private final PlatformSupportService platformSupportService;

    @Value("${app.scooter.low-battery-threshold:20}")
    private int lowBatteryThreshold;

    @Value("${app.scooter.battery-drain-per-minute:1}")
    private int batteryDrainPerMinute;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateBookingVo createBooking(String userId, CreateBookingRequest request) {
        HireOptionEntity hireOption = findHireOption(request.getHireOptionId());
        ScooterEntity scooter = requireAvailableScooter(request.getScooterId());
        LocalDateTime startAt = request.getPlannedStartAt() == null ? LocalDateTime.now() : request.getPlannedStartAt();
        PriceSnapshot priceSnapshot = calculatePrice(userId, hireOption.getBasePrice());

        reserveScooter(scooter.getScooterId());
        BookingEntity booking = createBookingEntity(
                DomainConstants.CUSTOMER_TYPE_REGISTERED,
                userId,
                null,
                null,
                null,
                scooter.getScooterId(),
                hireOption,
                startAt,
                priceSnapshot,
                DomainConstants.ROLE_CUSTOMER);
        bookingMapper.insert(booking);
        platformSupportService.recordBookingEvent(booking.getBookingId(), "BOOKING_CREATED", userId, DomainConstants.ROLE_CUSTOMER,
                "Initial booking created and awaiting payment");

        CreateBookingVo response = new CreateBookingVo();
        response.setBookingId(booking.getBookingId());
        response.setStatus(booking.getStatus());
        response.setPaymentStatus(booking.getPaymentStatus());
        response.setScooterStatusSnapshot(DomainConstants.ScooterStatus.RESERVED);
        response.setStartAt(booking.getStartAt());
        response.setEndAt(booking.getEndAt());
        response.setPriceBreakdown(toPriceBreakdown(booking.getPriceBase(), booking.getPriceDiscount(), booking.getPriceFinal()));
        return response;
    }

    @Override
    public List<BookingListItemVo> listBookings(String userId, String status) {
        LambdaQueryWrapper<BookingEntity> query = new LambdaQueryWrapper<BookingEntity>()
                .eq(BookingEntity::getUserId, userId)
                .orderByDesc(BookingEntity::getUpdatedAt);
        if (hasText(status)) {
            query.eq(BookingEntity::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        return bookingMapper.selectList(query).stream().map(this::toListItemVo).toList();
    }

    @Override
    public BookingDetailVo getBookingDetail(String userId, String bookingId) {
        return toDetailVo(requireAccessibleBooking(userId, DomainConstants.ROLE_CUSTOMER, bookingId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingDetailVo updateBooking(String userId, String bookingId, UpdateBookingRequest request) {
        if (request == null || !request.hasAnyField()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "At least one field is required: scooterId, hireOptionId, plannedStartAt");
        }

        BookingEntity booking = requireAccessibleBooking(userId, DomainConstants.ROLE_CUSTOMER, bookingId);
        if (!CUSTOMER_MUTABLE_STATUSES.contains(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.BOOKING_CONFLICT,
                    "Booking cannot be modified in current state");
        }

        HireOptionEntity hireOption = hasText(request.getHireOptionId())
                ? findHireOption(request.getHireOptionId())
                : findHireOptionByStoredId(booking.getHireOptionId());
        LocalDateTime startAt = request.getPlannedStartAt() == null ? booking.getStartAt() : request.getPlannedStartAt();
        PriceSnapshot priceSnapshot = calculatePrice(booking.getUserId(), hireOption.getBasePrice());

        String scooterId = booking.getScooterId();
        if (hasText(request.getScooterId())) {
            String requestedScooterId = normalizeScooterId(request.getScooterId());
            if (!requestedScooterId.equals(booking.getScooterId())) {
                reserveScooter(requestedScooterId);
                releaseScooter(booking.getScooterId());
                scooterId = requestedScooterId;
            }
        }

        booking.setScooterId(scooterId);
        booking.setHireOptionId(hireOption.getHireOptionId());
        booking.setStartAt(startAt);
        booking.setEndAt(startAt.plusMinutes(hireOption.getDurationMinutes()));
        booking.setPriceBase(hireOption.getBasePrice());
        booking.setPriceDiscount(priceSnapshot.discount());
        booking.setPriceFinal(priceSnapshot.finalPrice());
        booking.setStatus(DomainConstants.BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(DomainConstants.PAYMENT_STATUS_UNPAID);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.updateById(booking);
        platformSupportService.recordBookingEvent(booking.getBookingId(), "BOOKING_UPDATED", userId, DomainConstants.ROLE_CUSTOMER,
                "Booking details updated and payment needs reconfirmation");
        return toDetailVo(booking);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CancelBookingVo cancelBooking(String userId, String bookingId, CancelBookingRequest request) {
        BookingEntity booking = requireAccessibleBooking(userId, DomainConstants.ROLE_CUSTOMER, bookingId);
        if (DomainConstants.BookingStatus.CANCELLED.equals(booking.getStatus())) {
            CancelBookingVo alreadyCancelled = new CancelBookingVo();
            alreadyCancelled.setBookingId(booking.getBookingId());
            alreadyCancelled.setStatus(booking.getStatus());
            alreadyCancelled.setCancelledAt(booking.getUpdatedAt());
            return alreadyCancelled;
        }

        if (!Set.of(DomainConstants.BookingStatus.PENDING_PAYMENT, DomainConstants.BookingStatus.CONFIRMED).contains(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.BOOKING_CONFLICT, "Booking cannot be cancelled in current state");
        }

        booking.setStatus(DomainConstants.BookingStatus.CANCELLED);
        booking.setCancelReason(request == null ? null : request.getReason());
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.updateById(booking);
        releaseScooter(booking.getScooterId());
        platformSupportService.recordBookingEvent(booking.getBookingId(), "BOOKING_CANCELLED", userId, DomainConstants.ROLE_CUSTOMER,
                booking.getCancelReason());
        platformSupportService.createNotification(booking.getUserId(), DomainConstants.NotificationType.BOOKING_CANCELLED,
                "Booking cancelled", "Booking " + booking.getBookingRef() + " was cancelled", booking.getBookingId());

        CancelBookingVo response = new CancelBookingVo();
        response.setBookingId(booking.getBookingId());
        response.setStatus(DomainConstants.BookingStatus.CANCELLED);
        response.setCancelledAt(LocalDateTime.now());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startBooking(String userId, String role, String bookingId) {
        BookingEntity booking = requireAccessibleBooking(userId, role, bookingId);
        if (!DomainConstants.BookingStatus.CONFIRMED.equals(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.BOOKING_CONFLICT, "Booking cannot be started in current state");
        }
        booking.setStatus(DomainConstants.BookingStatus.ACTIVE);
        booking.setActualStartAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.updateById(booking);
        setScooterStatus(booking.getScooterId(), DomainConstants.ScooterStatus.IN_USE);
        platformSupportService.recordBookingEvent(booking.getBookingId(), "BOOKING_STARTED", userId, role, "Ride started");
        return toBookingMap(booking);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> endBooking(String userId, String role, String bookingId) {
        BookingEntity booking = requireAccessibleBooking(userId, role, bookingId);
        if (!DomainConstants.BookingStatus.ACTIVE.equals(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.BOOKING_CONFLICT, "Booking cannot be ended in current state");
        }
        scooterService.processScooterLifecycle();
        refreshScooterBatteryIfNeeded(booking.getScooterId());
        booking.setStatus(DomainConstants.BookingStatus.COMPLETED);
        booking.setActualEndAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.updateById(booking);
        setScooterStatus(booking.getScooterId(), resolveReadyScooterStatus(booking.getScooterId()));
        platformSupportService.recordBookingEvent(booking.getBookingId(), "BOOKING_COMPLETED", userId, role, "Ride ended");
        return toBookingMap(booking);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> extendBooking(String userId, String role, String bookingId, Map<String, Object> request) {
        BookingEntity booking = requireAccessibleBooking(userId, role, bookingId);
        if (!Set.of(DomainConstants.BookingStatus.CONFIRMED, DomainConstants.BookingStatus.ACTIVE).contains(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.BOOKING_CONFLICT, "Booking cannot be extended in current state");
        }

        String additionalHireOption = request == null ? null : stringValue(request.get("additionalHireOptionCode"));
        if (!hasText(additionalHireOption)) {
            additionalHireOption = request == null ? null : stringValue(request.get("additionalHireOptionId"));
        }
        HireOptionEntity extensionOption = findHireOption(additionalHireOption);
        PriceSnapshot extensionPrice = calculatePrice(booking.getUserId(), extensionOption.getBasePrice());

        LocalDateTime oldEndAt = booking.getEndAt();
        booking.setEndAt(booking.getEndAt().plusMinutes(extensionOption.getDurationMinutes()));
        booking.setPriceBase(booking.getPriceBase().add(extensionOption.getBasePrice()));
        booking.setPriceDiscount(booking.getPriceDiscount().add(extensionPrice.discount()));
        booking.setPriceFinal(booking.getPriceFinal().add(extensionPrice.finalPrice()));
        if (DomainConstants.PAYMENT_STATUS_PAID.equals(booking.getPaymentStatus())) {
            booking.setPaymentStatus(DomainConstants.PAYMENT_STATUS_PARTIAL);
        }
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.updateById(booking);
        platformSupportService.recordBookingEvent(booking.getBookingId(), "BOOKING_EXTENDED", userId, role,
                "Extended by " + extensionOption.getCode());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bookingId", booking.getBookingId());
        data.put("oldEndAt", oldEndAt);
        data.put("newEndAt", booking.getEndAt());
        data.put("additionalCharge", extensionPrice.finalPrice());
        data.put("paymentStatus", booking.getPaymentStatus());
        return data;
    }

    @Override
    public List<Map<String, Object>> getBookingTimeline(String userId, String role, String bookingId) {
        requireAccessibleBooking(userId, role, bookingId);
        return platformSupportService.listBookingTimeline(bookingId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createGuestBooking(String staffUserId, Map<String, Object> request) {
        HireOptionEntity hireOption = findHireOption(stringValue(request.get("hireOptionId")));
        ScooterEntity scooter = requireAvailableScooter(stringValue(request.get("scooterId")));
        LocalDateTime startAt = request != null && request.get("plannedStartAt") != null
                ? LocalDateTime.parse(String.valueOf(request.get("plannedStartAt")))
                : LocalDateTime.now();

        reserveScooter(scooter.getScooterId());
        BookingEntity booking = createBookingEntity(
                DomainConstants.CUSTOMER_TYPE_GUEST,
                null,
                requireText(request.get("guestName"), "guestName"),
                requireText(request.get("guestEmail"), "guestEmail"),
                stringValue(request.get("guestPhone")),
                scooter.getScooterId(),
                hireOption,
                startAt,
                new PriceSnapshot(hireOption.getBasePrice(), BigDecimal.ZERO, hireOption.getBasePrice()),
                DomainConstants.ROLE_STAFF);
        bookingMapper.insert(booking);
        platformSupportService.recordBookingEvent(booking.getBookingId(), "BOOKING_CREATED", staffUserId, DomainConstants.ROLE_STAFF,
                "Guest booking created");
        return toBookingMap(booking);
    }

    @Override
    public Map<String, Object> getGuestBookingDetail(String staffUserId, String bookingId) {
        BookingEntity booking = requireBooking(bookingId);
        if (!DomainConstants.CUSTOMER_TYPE_GUEST.equals(booking.getCustomerType())) {
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), ErrorCodes.AUTH_FORBIDDEN, "Not a guest booking");
        }
        return toBookingMap(booking);
    }

    @Override
    public List<Map<String, Object>> listAdminBookings(String status, String paymentStatus, String customerType) {
        LambdaQueryWrapper<BookingEntity> query = new LambdaQueryWrapper<BookingEntity>()
                .orderByDesc(BookingEntity::getCreatedAt);
        if (hasText(status)) {
            query.eq(BookingEntity::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(paymentStatus)) {
            query.eq(BookingEntity::getPaymentStatus, paymentStatus.trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(customerType)) {
            query.eq(BookingEntity::getCustomerType, customerType.trim().toUpperCase(Locale.ROOT));
        }
        return bookingMapper.selectList(query).stream().map(this::toBookingMap).toList();
    }

    @Override
    public Map<String, Object> getAdminBookingDetail(String bookingId) {
        BookingEntity booking = requireBooking(bookingId);
        Map<String, Object> data = toBookingMap(booking);
        data.put("timeline", platformSupportService.listBookingTimeline(bookingId));
        data.put("payments", paymentMapper.selectList(new LambdaQueryWrapper<PaymentEntity>()
                .eq(PaymentEntity::getBookingId, bookingId)
                .orderByDesc(PaymentEntity::getCreatedAt)).stream().map(this::toPaymentMap).toList());
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> overrideBooking(String bookingId, Map<String, Object> request) {
        BookingEntity booking = requireBooking(bookingId);
        if (request.containsKey("scooterId")) {
            String newScooterId = normalizeScooterId(stringValue(request.get("scooterId")));
            if (!newScooterId.equals(booking.getScooterId())) {
                reserveScooter(newScooterId);
                releaseScooter(booking.getScooterId());
                booking.setScooterId(newScooterId);
            }
        }
        if (request.containsKey("status")) {
            String status = normalizeBookingStatus(stringValue(request.get("status")));
            booking.setStatus(status);
            if (DomainConstants.BookingStatus.CANCELLED.equals(status)) {
                releaseScooter(booking.getScooterId());
            }
            if (DomainConstants.BookingStatus.ACTIVE.equals(status)) {
                setScooterStatus(booking.getScooterId(), DomainConstants.ScooterStatus.IN_USE);
                if (booking.getActualStartAt() == null) {
                    booking.setActualStartAt(LocalDateTime.now());
                }
            }
            if (DomainConstants.BookingStatus.COMPLETED.equals(status)) {
                scooterService.processScooterLifecycle();
                refreshScooterBatteryIfNeeded(booking.getScooterId());
                setScooterStatus(booking.getScooterId(), resolveReadyScooterStatus(booking.getScooterId()));
                if (booking.getActualEndAt() == null) {
                    booking.setActualEndAt(LocalDateTime.now());
                }
            }
        }
        if (request.containsKey("paymentStatus")) {
            booking.setPaymentStatus(normalizePaymentStatus(stringValue(request.get("paymentStatus"))));
        }
        if (request.containsKey("cancelReason")) {
            booking.setCancelReason(stringValue(request.get("cancelReason")));
        }
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.updateById(booking);
        platformSupportService.recordBookingEvent(booking.getBookingId(), "BOOKING_OVERRIDDEN", null, DomainConstants.ROLE_MANAGER,
                "Manager override applied");
        platformSupportService.recordAudit("BOOKING_OVERRIDDEN", "BOOKING", bookingId, "Manager override applied");
        return getAdminBookingDetail(bookingId);
    }

    @Override
    public Map<String, Object> getBookingConfirmation(String userId, String role, String bookingId) {
        BookingEntity booking = requireAccessibleBooking(userId, role, bookingId);
        BookingConfirmationEntity confirmation = bookingConfirmationMapper.selectOne(new LambdaQueryWrapper<BookingConfirmationEntity>()
                .eq(BookingConfirmationEntity::getBookingId, booking.getBookingId())
                .orderByDesc(BookingConfirmationEntity::getUpdatedAt)
                .last("LIMIT 1"));
        return platformSupportService.toConfirmationMap(confirmation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> resendBookingConfirmation(String userId, String role, String bookingId) {
        BookingEntity booking = requireAccessibleBooking(userId, role, bookingId);
        String recipientEmail = resolveConfirmationRecipient(booking);
        BookingConfirmationEntity confirmation = platformSupportService.createOrRefreshConfirmation(
                booking,
                recipientEmail,
                "EMAIL",
                "Booking confirmation for " + booking.getBookingRef(),
                true);
        if (booking.getUserId() != null) {
            platformSupportService.createNotification(booking.getUserId(), DomainConstants.NotificationType.BOOKING_CONFIRMATION,
                    "Booking confirmation resent", "Confirmation resent for booking " + booking.getBookingRef(), booking.getBookingId());
        }
        return platformSupportService.toConfirmationMap(confirmation);
    }

    @Override
    public List<Map<String, Object>> listConfirmations(String userId) {
        return bookingConfirmationMapper.selectList(new LambdaQueryWrapper<BookingConfirmationEntity>()
                        .eq(BookingConfirmationEntity::getUserId, userId)
                        .orderByDesc(BookingConfirmationEntity::getUpdatedAt))
                .stream()
                .map(platformSupportService::toConfirmationMap)
                .toList();
    }

    private BookingEntity createBookingEntity(String customerType,
                                              String userId,
                                              String guestName,
                                              String guestEmail,
                                              String guestPhone,
                                              String scooterId,
                                              HireOptionEntity hireOption,
                                              LocalDateTime startAt,
                                              PriceSnapshot priceSnapshot,
                                              String createdByRole) {
        BookingEntity booking = new BookingEntity();
        booking.setBookingId(newBookingId());
        booking.setBookingRef(newBookingRef());
        booking.setCustomerType(customerType);
        booking.setUserId(userId);
        booking.setGuestName(trimToNull(guestName));
        booking.setGuestEmail(trimToNull(guestEmail));
        booking.setGuestPhone(trimToNull(guestPhone));
        booking.setScooterId(scooterId);
        booking.setHireOptionId(hireOption.getHireOptionId());
        booking.setStartAt(startAt);
        booking.setEndAt(startAt.plusMinutes(hireOption.getDurationMinutes()));
        booking.setStatus(DomainConstants.BookingStatus.PENDING_PAYMENT);
        booking.setPriceBase(hireOption.getBasePrice());
        booking.setPriceDiscount(priceSnapshot.discount());
        booking.setPriceFinal(priceSnapshot.finalPrice());
        booking.setPaymentStatus(DomainConstants.PAYMENT_STATUS_UNPAID);
        booking.setCreatedByRole(createdByRole);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        return booking;
    }

    private PriceSnapshot calculatePrice(String userId, BigDecimal basePrice) {
        DiscountRuleService.DiscountComputation computation = discountRuleService.calculateForUser(userId, basePrice);
        return new PriceSnapshot(basePrice, computation.totalDiscount(), computation.finalPrice());
    }

    private HireOptionEntity findHireOption(String input) {
        if (!hasText(input)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "hireOptionId is required");
        }
        HireOptionEntity hireOption = hireOptionMapper.selectOne(new LambdaQueryWrapper<HireOptionEntity>()
                .eq(HireOptionEntity::getActive, 1)
                .and(wrapper -> wrapper.eq(HireOptionEntity::getHireOptionId, input)
                        .or()
                        .eq(HireOptionEntity::getCode, input)));
        if (hireOption == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Hire option not found");
        }
        return hireOption;
    }

    private HireOptionEntity findHireOptionByStoredId(String hireOptionId) {
        HireOptionEntity hireOption = hireOptionMapper.selectOne(new LambdaQueryWrapper<HireOptionEntity>()
                .eq(HireOptionEntity::getActive, 1)
                .eq(HireOptionEntity::getHireOptionId, hireOptionId));
        if (hireOption == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Hire option not found");
        }
        return hireOption;
    }

    private BookingEntity requireAccessibleBooking(String userId, String role, String bookingId) {
        BookingEntity booking = requireBooking(bookingId);
        if (DomainConstants.ROLE_MANAGER.equals(role) || DomainConstants.ROLE_STAFF.equals(role)) {
            return booking;
        }
        if (!userId.equals(booking.getUserId())) {
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

    private ScooterEntity requireAvailableScooter(String scooterId) {
        ScooterEntity scooter = scooterMapper.selectOne(new LambdaQueryWrapper<ScooterEntity>()
                .eq(ScooterEntity::getScooterId, normalizeScooterId(scooterId)));
        if (scooter == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Scooter not found");
        }
        if (!DomainConstants.ScooterStatus.AVAILABLE.equals(scooter.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.SCOOTER_NOT_AVAILABLE, "Scooter is no longer available");
        }
        return scooter;
    }

    private void reserveScooter(String scooterId) {
        UpdateWrapper<ScooterEntity> reserveUpdate = new UpdateWrapper<>();
        reserveUpdate.eq("scooter_id", normalizeScooterId(scooterId))
                .eq("status", DomainConstants.ScooterStatus.AVAILABLE)
                .set("status", DomainConstants.ScooterStatus.RESERVED)
                .set("updated_at", LocalDateTime.now())
                .setSql("version = version + 1");
        int reserveRows = scooterMapper.update(null, reserveUpdate);
        if (reserveRows == 0) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.SCOOTER_NOT_AVAILABLE, "Scooter is no longer available");
        }
    }

    private void releaseScooter(String scooterId) {
        String readyStatus = resolveReadyScooterStatus(scooterId);
        UpdateWrapper<ScooterEntity> releaseUpdate = new UpdateWrapper<>();
        releaseUpdate.eq("scooter_id", normalizeScooterId(scooterId))
                .in("status", DomainConstants.ScooterStatus.RESERVED, DomainConstants.ScooterStatus.IN_USE)
                .set("status", readyStatus)
                .set("charge_started_at", null)
                .set("updated_at", LocalDateTime.now())
                .setSql("version = version + 1");
        scooterMapper.update(null, releaseUpdate);
    }

    private void setScooterStatus(String scooterId, String status) {
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<ScooterEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("scooter_id", normalizeScooterId(scooterId))
                .set("status", status)
                .set("updated_at", now)
                .set("charge_started_at", DomainConstants.ScooterStatus.CHARGING.equals(status) ? now : null)
                .setSql("version = version + 1");
        if (DomainConstants.ScooterStatus.IN_USE.equals(status) || DomainConstants.ScooterStatus.CHARGING.equals(status)) {
            updateWrapper.set("battery_updated_at", now);
        }
        scooterMapper.update(null, updateWrapper);
    }

    private String resolveReadyScooterStatus(String scooterId) {
        ScooterEntity scooter = scooterMapper.selectOne(new LambdaQueryWrapper<ScooterEntity>()
                .eq(ScooterEntity::getScooterId, normalizeScooterId(scooterId)));
        if (scooter == null) {
            return DomainConstants.ScooterStatus.AVAILABLE;
        }
        Integer batteryPercent = scooter.getBatteryPercent();
        return batteryPercent != null && batteryPercent < lowBatteryThreshold
                ? DomainConstants.ScooterStatus.LOW_BATTERY
                : DomainConstants.ScooterStatus.AVAILABLE;
    }

    private void refreshScooterBatteryIfNeeded(String scooterId) {
        ScooterEntity scooter = scooterMapper.selectOne(new LambdaQueryWrapper<ScooterEntity>()
                .eq(ScooterEntity::getScooterId, normalizeScooterId(scooterId)));
        if (scooter == null || !DomainConstants.ScooterStatus.IN_USE.equals(scooter.getStatus())) {
            return;
        }
        LocalDateTime lastTick = scooter.getBatteryUpdatedAt();
        if (lastTick == null) {
            scooter.setBatteryUpdatedAt(LocalDateTime.now());
            scooterMapper.updateById(scooter);
            return;
        }
        long elapsedMinutes = Duration.between(lastTick, LocalDateTime.now()).toMinutes();
        if (elapsedMinutes <= 0) {
            return;
        }
        int currentBattery = scooter.getBatteryPercent() == null ? 100 : scooter.getBatteryPercent();
        int newBattery = Math.max(0, currentBattery - Math.toIntExact(elapsedMinutes * Math.max(1, batteryDrainPerMinute)));
        scooter.setBatteryPercent(newBattery);
        scooter.setBatteryUpdatedAt(lastTick.plusMinutes(elapsedMinutes));
        scooter.setUpdatedAt(LocalDateTime.now());
        scooterMapper.updateById(scooter);
    }

    private String normalizeScooterId(String scooterId) {
        if (!hasText(scooterId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "scooterId is required");
        }
        return scooterId.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeBookingStatus(String status) {
        String normalized = requireText(status, "status").toUpperCase(Locale.ROOT);
        if (!Set.of(
                DomainConstants.BookingStatus.PENDING_PAYMENT,
                DomainConstants.BookingStatus.CONFIRMED,
                DomainConstants.BookingStatus.ACTIVE,
                DomainConstants.BookingStatus.COMPLETED,
                DomainConstants.BookingStatus.CANCELLED,
                DomainConstants.BookingStatus.EXPIRED
        ).contains(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "Invalid booking status");
        }
        return normalized;
    }

    private String normalizePaymentStatus(String status) {
        String normalized = requireText(status, "paymentStatus").toUpperCase(Locale.ROOT);
        if (!Set.of(
                DomainConstants.PAYMENT_STATUS_UNPAID,
                DomainConstants.PAYMENT_STATUS_PAID,
                DomainConstants.PAYMENT_STATUS_PARTIAL,
                DomainConstants.PAYMENT_STATUS_REFUNDED
        ).contains(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, "Invalid payment status");
        }
        return normalized;
    }

    private String requireText(String value, String field) {
        if (!hasText(value)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " is required");
        }
        return value.trim();
    }

    private String requireText(Object value, String field) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR, field + " is required");
        }
        return String.valueOf(value).trim();
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

    private String newBookingId() {
        return "BKG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String newBookingRef() {
        return "REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }

    private PriceBreakdownVo toPriceBreakdown(BigDecimal base, BigDecimal discount, BigDecimal finalPrice) {
        PriceBreakdownVo breakdown = new PriceBreakdownVo();
        breakdown.setBase(base);
        breakdown.setDiscount(discount);
        breakdown.setFinalPrice(finalPrice);
        return breakdown;
    }

    private BookingListItemVo toListItemVo(BookingEntity booking) {
        BookingListItemVo vo = new BookingListItemVo();
        vo.setBookingId(booking.getBookingId());
        vo.setBookingRef(booking.getBookingRef());
        vo.setCustomerType(booking.getCustomerType());
        vo.setScooterId(booking.getScooterId());
        vo.setHireOptionId(booking.getHireOptionId());
        vo.setStatus(booking.getStatus());
        vo.setStartAt(booking.getStartAt());
        vo.setEndAt(booking.getEndAt());
        vo.setPriceFinal(booking.getPriceFinal());
        vo.setPaymentStatus(booking.getPaymentStatus());
        vo.setUpdatedAt(booking.getUpdatedAt());
        return vo;
    }

    private BookingDetailVo toDetailVo(BookingEntity booking) {
        BookingDetailVo vo = new BookingDetailVo();
        vo.setBookingId(booking.getBookingId());
        vo.setBookingRef(booking.getBookingRef());
        vo.setCustomerType(booking.getCustomerType());
        vo.setUserId(booking.getUserId());
        vo.setGuestName(booking.getGuestName());
        vo.setGuestEmail(booking.getGuestEmail());
        vo.setGuestPhone(booking.getGuestPhone());
        vo.setScooterId(booking.getScooterId());
        vo.setHireOptionId(booking.getHireOptionId());
        vo.setStatus(booking.getStatus());
        vo.setStartAt(booking.getStartAt());
        vo.setEndAt(booking.getEndAt());
        vo.setActualStartAt(booking.getActualStartAt());
        vo.setActualEndAt(booking.getActualEndAt());
        vo.setPriceBase(booking.getPriceBase());
        vo.setPriceDiscount(booking.getPriceDiscount());
        vo.setPriceFinal(booking.getPriceFinal());
        vo.setPaymentStatus(booking.getPaymentStatus());
        vo.setCancelReason(booking.getCancelReason());
        vo.setCreatedAt(booking.getCreatedAt());
        vo.setUpdatedAt(booking.getUpdatedAt());
        return vo;
    }

    private Map<String, Object> toBookingMap(BookingEntity booking) {
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
        data.put("status", booking.getStatus());
        data.put("startAt", booking.getStartAt());
        data.put("endAt", booking.getEndAt());
        data.put("actualStartAt", booking.getActualStartAt());
        data.put("actualEndAt", booking.getActualEndAt());
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

    private String resolveConfirmationRecipient(BookingEntity booking) {
        if (hasText(booking.getGuestEmail())) {
            return booking.getGuestEmail();
        }
        BookingConfirmationEntity latest = bookingConfirmationMapper.selectOne(new LambdaQueryWrapper<BookingConfirmationEntity>()
                .eq(BookingConfirmationEntity::getBookingId, booking.getBookingId())
                .orderByDesc(BookingConfirmationEntity::getUpdatedAt)
                .last("LIMIT 1"));
        return latest == null ? null : latest.getRecipientEmail();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record PriceSnapshot(BigDecimal basePrice, BigDecimal discount, BigDecimal finalPrice) {
    }
}
