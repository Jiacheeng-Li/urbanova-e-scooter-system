package com.lcyhz.urbanova.vo.booking;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CancelBookingVo {
    private String bookingId;
    private String status;
    private LocalDateTime cancelledAt;
}

