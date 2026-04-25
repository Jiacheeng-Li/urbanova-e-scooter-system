package com.lcyhz.urbanova.vo.booking;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateBookingVo {
    private String bookingId;
    private String status;
    private String paymentStatus;
    private String scooterStatusSnapshot;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private PriceBreakdownVo priceBreakdown;
}

