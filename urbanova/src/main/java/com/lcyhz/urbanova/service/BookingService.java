package com.lcyhz.urbanova.service;

import com.lcyhz.urbanova.dto.booking.CancelBookingRequest;
import com.lcyhz.urbanova.dto.booking.CreateBookingRequest;
import com.lcyhz.urbanova.dto.booking.UpdateBookingRequest;
import com.lcyhz.urbanova.vo.booking.CancelBookingVo;
import com.lcyhz.urbanova.vo.booking.BookingDetailVo;
import com.lcyhz.urbanova.vo.booking.BookingListItemVo;
import com.lcyhz.urbanova.vo.booking.CreateBookingVo;

import java.util.List;

public interface BookingService {
    CreateBookingVo createBooking(String userId, CreateBookingRequest request);

    List<BookingListItemVo> listBookings(String userId, String status);

    BookingDetailVo getBookingDetail(String userId, String bookingId);

    BookingDetailVo updateBooking(String userId, String bookingId, UpdateBookingRequest request);

    CancelBookingVo cancelBooking(String userId, String bookingId, CancelBookingRequest request);
}
