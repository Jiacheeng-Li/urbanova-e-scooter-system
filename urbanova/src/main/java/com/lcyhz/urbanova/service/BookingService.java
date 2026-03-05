package com.lcyhz.urbanova.service;

import com.lcyhz.urbanova.dto.booking.CancelBookingRequest;
import com.lcyhz.urbanova.dto.booking.CreateBookingRequest;
import com.lcyhz.urbanova.vo.booking.CancelBookingVo;
import com.lcyhz.urbanova.vo.booking.CreateBookingVo;

public interface BookingService {
    CreateBookingVo createBooking(String userId, CreateBookingRequest request);

    CancelBookingVo cancelBooking(String userId, String bookingId, CancelBookingRequest request);
}

