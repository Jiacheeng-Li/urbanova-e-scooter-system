package com.lcyhz.urbanova.vo.booking;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingListItemVo {
    private String bookingId;
    private String bookingRef;
    private String scooterId;
    private String hireOptionId;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private BigDecimal priceFinal;
    private String paymentStatus;
    private LocalDateTime updatedAt;
}

