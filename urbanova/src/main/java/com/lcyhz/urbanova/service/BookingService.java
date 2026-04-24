package com.lcyhz.urbanova.service;

import com.lcyhz.urbanova.dto.booking.CancelBookingRequest;
import com.lcyhz.urbanova.dto.booking.CreateBookingRequest;
import com.lcyhz.urbanova.dto.booking.UpdateBookingRequest;
import com.lcyhz.urbanova.vo.booking.BookingDetailVo;
import com.lcyhz.urbanova.vo.booking.BookingListItemVo;
import com.lcyhz.urbanova.vo.booking.CancelBookingVo;
import com.lcyhz.urbanova.vo.booking.CreateBookingVo;

import java.util.List;
import java.util.Map;

public interface BookingService {
    CreateBookingVo createBooking(String userId, CreateBookingRequest request);

    List<BookingListItemVo> listBookings(String userId, String status);

    BookingDetailVo getBookingDetail(String userId, String bookingId);

    BookingDetailVo updateBooking(String userId, String bookingId, UpdateBookingRequest request);

    CancelBookingVo cancelBooking(String userId, String bookingId, CancelBookingRequest request);

    Map<String, Object> startBooking(String userId, String role, String bookingId);

    Map<String, Object> endBooking(String userId, String role, String bookingId);

    Map<String, Object> extendBooking(String userId, String role, String bookingId, Map<String, Object> request);

    List<Map<String, Object>> getBookingTimeline(String userId, String role, String bookingId);

    Map<String, Object> createGuestBooking(String staffUserId, Map<String, Object> request);

    Map<String, Object> getGuestBookingDetail(String staffUserId, String bookingId);

    List<Map<String, Object>> listAdminBookings(String status, String paymentStatus, String customerType);

    Map<String, Object> getAdminBookingDetail(String bookingId);

    Map<String, Object> overrideBooking(String bookingId, Map<String, Object> request);

    Map<String, Object> getBookingConfirmation(String userId, String role, String bookingId);

    Map<String, Object> resendBookingConfirmation(String userId, String role, String bookingId);

    List<Map<String, Object>> listConfirmations(String userId);
}
