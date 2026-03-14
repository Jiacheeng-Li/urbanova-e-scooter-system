package com.lcyhz.urbanova.vo.booking;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingDetailVo {
    private String bookingId;
    private String bookingRef;
    private String customerType;
    private String userId;
    private String scooterId;
    private String hireOptionId;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private BigDecimal priceBase;
    private BigDecimal priceDiscount;
    private BigDecimal priceFinal;
    private String paymentStatus;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

