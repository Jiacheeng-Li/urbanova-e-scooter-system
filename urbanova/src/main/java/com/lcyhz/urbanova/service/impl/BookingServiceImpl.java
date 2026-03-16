package com.lcyhz.urbanova.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.booking.CancelBookingRequest;
import com.lcyhz.urbanova.dto.booking.CreateBookingRequest;
import com.lcyhz.urbanova.dto.booking.UpdateBookingRequest;
import com.lcyhz.urbanova.entity.BookingEntity;
import com.lcyhz.urbanova.entity.HireOptionEntity;
import com.lcyhz.urbanova.entity.ScooterEntity;
import com.lcyhz.urbanova.mapper.BookingMapper;
import com.lcyhz.urbanova.mapper.HireOptionMapper;
import com.lcyhz.urbanova.mapper.ScooterMapper;
import com.lcyhz.urbanova.service.BookingService;
import com.lcyhz.urbanova.vo.booking.BookingDetailVo;
import com.lcyhz.urbanova.vo.booking.BookingListItemVo;
import com.lcyhz.urbanova.vo.booking.CancelBookingVo;
import com.lcyhz.urbanova.vo.booking.CreateBookingVo;
import com.lcyhz.urbanova.vo.booking.PriceBreakdownVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private static final Set<String> QUERYABLE_STATUSES = Set.of(
            DomainConstants.BookingStatus.CONFIRMED,
            DomainConstants.BookingStatus.CANCELLED
    );

    private final BookingMapper bookingMapper;
    private final HireOptionMapper hireOptionMapper;
    private final ScooterMapper scooterMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateBookingVo createBooking(String userId, CreateBookingRequest request) {
        HireOptionEntity hireOption = findHireOption(request.getHireOptionId());

        ScooterEntity scooter = scooterMapper.selectOne(new LambdaQueryWrapper<ScooterEntity>()
                .eq(ScooterEntity::getScooterId, request.getScooterId()));
        if (scooter == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Scooter not found");
        }
        if (!DomainConstants.ScooterStatus.AVAILABLE.equals(scooter.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.SCOOTER_NOT_AVAILABLE, "Scooter is no longer available");
        }

        LocalDateTime startAt = request.getPlannedStartAt() == null ? LocalDateTime.now() : request.getPlannedStartAt();
        LocalDateTime endAt = startAt.plusMinutes(hireOption.getDurationMinutes());
        BigDecimal basePrice = hireOption.getBasePrice();

        UpdateWrapper<ScooterEntity> reserveUpdate = new UpdateWrapper<>();
        reserveUpdate.eq("scooter_id", scooter.getScooterId())
                .eq("status", DomainConstants.ScooterStatus.AVAILABLE)
                .set("status", DomainConstants.ScooterStatus.RESERVED)
                .set("updated_at", LocalDateTime.now())
                .setSql("version = version + 1");
        int reserveRows = scooterMapper.update(null, reserveUpdate);
        if (reserveRows == 0) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.SCOOTER_NOT_AVAILABLE, "Scooter is no longer available");
        }

        BookingEntity booking = new BookingEntity();
        booking.setBookingId(newBookingId());
        booking.setBookingRef(newBookingRef());
        booking.setCustomerType(DomainConstants.CUSTOMER_TYPE_REGISTERED);
        booking.setUserId(userId);
        booking.setScooterId(scooter.getScooterId());
        booking.setHireOptionId(hireOption.getHireOptionId());
        booking.setStartAt(startAt);
        booking.setEndAt(endAt);
        booking.setStatus(DomainConstants.BookingStatus.CONFIRMED);
        booking.setPriceBase(basePrice);
        booking.setPriceDiscount(BigDecimal.ZERO);
        booking.setPriceFinal(basePrice);
        booking.setPaymentStatus(DomainConstants.PAYMENT_STATUS_UNPAID);
        booking.setCreatedByRole(DomainConstants.ROLE_CUSTOMER);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.insert(booking);

        CreateBookingVo response = new CreateBookingVo();
        response.setBookingId(booking.getBookingId());
        response.setStatus(booking.getStatus());
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
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
            if (!QUERYABLE_STATUSES.contains(normalizedStatus)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                        "status must be CONFIRMED or CANCELLED");
            }
            query.eq(BookingEntity::getStatus, normalizedStatus);
        }

        return bookingMapper.selectList(query).stream().map(this::toListItemVo).toList();
    }

    @Override
    public BookingDetailVo getBookingDetail(String userId, String bookingId) {
        BookingEntity booking = findOwnedBooking(userId, bookingId);
        return toDetailVo(booking);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingDetailVo updateBooking(String userId, String bookingId, UpdateBookingRequest request) {
        if (request == null || !request.hasAnyField()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    "At least one field is required: scooterId, hireOptionId, plannedStartAt");
        }

        BookingEntity booking = findOwnedBooking(userId, bookingId);
        if (!DomainConstants.BookingStatus.CONFIRMED.equals(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.BOOKING_CONFLICT,
                    "Booking cannot be modified in current state");
        }

        HireOptionEntity hireOption = hasText(request.getHireOptionId())
                ? findHireOption(request.getHireOptionId())
                : findHireOptionByStoredId(booking.getHireOptionId());
        LocalDateTime startAt = request.getPlannedStartAt() == null ? booking.getStartAt() : request.getPlannedStartAt();
        LocalDateTime endAt = startAt.plusMinutes(hireOption.getDurationMinutes());
        BigDecimal basePrice = hireOption.getBasePrice();

        String scooterId = booking.getScooterId();
        if (hasText(request.getScooterId())) {
            String requestedScooterId = request.getScooterId().trim();
            if (!requestedScooterId.equals(booking.getScooterId())) {
                reserveScooter(requestedScooterId);
                releaseScooter(booking.getScooterId());
                scooterId = requestedScooterId;
            }
        }

        UpdateWrapper<BookingEntity> update = new UpdateWrapper<>();
        update.eq("booking_id", bookingId)
                .eq("user_id", userId)
                .eq("status", DomainConstants.BookingStatus.CONFIRMED)
                .set("scooter_id", scooterId)
                .set("hire_option_id", hireOption.getHireOptionId())
                .set("start_at", startAt)
                .set("end_at", endAt)
                .set("price_base", basePrice)
                .set("price_discount", BigDecimal.ZERO)
                .set("price_final", basePrice)
                .set("updated_at", LocalDateTime.now());
        int updateRows = bookingMapper.update(null, update);
        if (updateRows == 0) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.BOOKING_CONFLICT,
                    "Booking state changed, please retry");
        }

        return getBookingDetail(userId, bookingId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CancelBookingVo cancelBooking(String userId, String bookingId, CancelBookingRequest request) {
        BookingEntity booking = findOwnedBooking(userId, bookingId);

        if (DomainConstants.BookingStatus.CANCELLED.equals(booking.getStatus())) {
            CancelBookingVo alreadyCancelled = new CancelBookingVo();
            alreadyCancelled.setBookingId(booking.getBookingId());
            alreadyCancelled.setStatus(booking.getStatus());
            alreadyCancelled.setCancelledAt(booking.getUpdatedAt());
            return alreadyCancelled;
        }

        if (!DomainConstants.BookingStatus.CONFIRMED.equals(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.BOOKING_CONFLICT, "Booking cannot be cancelled in current state");
        }

        UpdateWrapper<BookingEntity> cancelUpdate = new UpdateWrapper<>();
        cancelUpdate.eq("booking_id", bookingId)
                .eq("status", DomainConstants.BookingStatus.CONFIRMED)
                .set("status", DomainConstants.BookingStatus.CANCELLED)
                .set("cancel_reason", request == null ? null : request.getReason())
                .set("updated_at", LocalDateTime.now());
        int cancelRows = bookingMapper.update(null, cancelUpdate);
        if (cancelRows == 0) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.BOOKING_CONFLICT, "Booking state changed, please retry");
        }

        UpdateWrapper<ScooterEntity> releaseUpdate = new UpdateWrapper<>();
        releaseUpdate.eq("scooter_id", booking.getScooterId())
                .eq("status", DomainConstants.ScooterStatus.RESERVED)
                .set("status", DomainConstants.ScooterStatus.AVAILABLE)
                .set("updated_at", LocalDateTime.now())
                .setSql("version = version + 1");
        scooterMapper.update(null, releaseUpdate);

        CancelBookingVo response = new CancelBookingVo();
        response.setBookingId(booking.getBookingId());
        response.setStatus(DomainConstants.BookingStatus.CANCELLED);
        response.setCancelledAt(LocalDateTime.now());
        return response;
    }

    private HireOptionEntity findHireOption(String input) {
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

    private BookingEntity findOwnedBooking(String userId, String bookingId) {
        BookingEntity booking = bookingMapper.selectOne(new LambdaQueryWrapper<BookingEntity>()
                .eq(BookingEntity::getBookingId, bookingId));
        if (booking == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Booking not found");
        }
        if (!userId.equals(booking.getUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), ErrorCodes.AUTH_FORBIDDEN, "No permission for this booking");
        }
        return booking;
    }

    private void reserveScooter(String scooterId) {
        ScooterEntity scooter = scooterMapper.selectOne(new LambdaQueryWrapper<ScooterEntity>()
                .eq(ScooterEntity::getScooterId, scooterId));
        if (scooter == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Scooter not found");
        }
        if (!DomainConstants.ScooterStatus.AVAILABLE.equals(scooter.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), ErrorCodes.SCOOTER_NOT_AVAILABLE, "Scooter is no longer available");
        }

        UpdateWrapper<ScooterEntity> reserveUpdate = new UpdateWrapper<>();
        reserveUpdate.eq("scooter_id", scooterId)
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
        UpdateWrapper<ScooterEntity> releaseUpdate = new UpdateWrapper<>();
        releaseUpdate.eq("scooter_id", scooterId)
                .eq("status", DomainConstants.ScooterStatus.RESERVED)
                .set("status", DomainConstants.ScooterStatus.AVAILABLE)
                .set("updated_at", LocalDateTime.now())
                .setSql("version = version + 1");
        scooterMapper.update(null, releaseUpdate);
    }

    private String newBookingId() {
        return "BKG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String newBookingRef() {
        return "REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
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
        vo.setScooterId(booking.getScooterId());
        vo.setHireOptionId(booking.getHireOptionId());
        vo.setStatus(booking.getStatus());
        vo.setStartAt(booking.getStartAt());
        vo.setEndAt(booking.getEndAt());
        vo.setPriceBase(booking.getPriceBase());
        vo.setPriceDiscount(booking.getPriceDiscount());
        vo.setPriceFinal(booking.getPriceFinal());
        vo.setPaymentStatus(booking.getPaymentStatus());
        vo.setCancelReason(booking.getCancelReason());
        vo.setCreatedAt(booking.getCreatedAt());
        vo.setUpdatedAt(booking.getUpdatedAt());
        return vo;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
