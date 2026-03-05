package com.lcyhz.urbanova.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.booking.CancelBookingRequest;
import com.lcyhz.urbanova.dto.booking.CreateBookingRequest;
import com.lcyhz.urbanova.entity.BookingEntity;
import com.lcyhz.urbanova.entity.HireOptionEntity;
import com.lcyhz.urbanova.entity.ScooterEntity;
import com.lcyhz.urbanova.mapper.BookingMapper;
import com.lcyhz.urbanova.mapper.HireOptionMapper;
import com.lcyhz.urbanova.mapper.ScooterMapper;
import com.lcyhz.urbanova.service.BookingService;
import com.lcyhz.urbanova.vo.booking.CancelBookingVo;
import com.lcyhz.urbanova.vo.booking.CreateBookingVo;
import com.lcyhz.urbanova.vo.booking.PriceBreakdownVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
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
    @Transactional(rollbackFor = Exception.class)
    public CancelBookingVo cancelBooking(String userId, String bookingId, CancelBookingRequest request) {
        BookingEntity booking = bookingMapper.selectOne(new LambdaQueryWrapper<BookingEntity>()
                .eq(BookingEntity::getBookingId, bookingId));
        if (booking == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Booking not found");
        }
        if (!userId.equals(booking.getUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), ErrorCodes.AUTH_FORBIDDEN, "No permission for this booking");
        }

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
}

