package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.dto.booking.CancelBookingRequest;
import com.lcyhz.urbanova.dto.booking.CreateBookingRequest;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.BookingService;
import com.lcyhz.urbanova.vo.booking.CancelBookingVo;
import com.lcyhz.urbanova.vo.booking.CreateBookingVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping("/bookings")
    public ApiResponse<CreateBookingVo> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        String userId = AuthContext.getRequiredUserId();
        return ApiResponse.success(bookingService.createBooking(userId, request));
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public ApiResponse<CancelBookingVo> cancelBooking(@PathVariable String bookingId,
                                                      @Valid @RequestBody(required = false) CancelBookingRequest request) {
        String userId = AuthContext.getRequiredUserId();
        CancelBookingRequest actualRequest = request == null ? new CancelBookingRequest() : request;
        return ApiResponse.success(bookingService.cancelBooking(userId, bookingId, actualRequest));
    }
}

