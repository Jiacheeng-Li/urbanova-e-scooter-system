package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConfirmationController {
    private final BookingService bookingService;

    @GetMapping("/bookings/{bookingId}/confirmation")
    public ApiResponse<Map<String, Object>> getBookingConfirmation(@PathVariable String bookingId) {
        return ApiResponse.success(bookingService.getBookingConfirmation(
                AuthContext.getRequiredUserId(),
                AuthContext.getRequiredUser().getRole(),
                bookingId));
    }

    @PostMapping("/bookings/{bookingId}/confirmation/resend")
    public ApiResponse<Map<String, Object>> resendBookingConfirmation(@PathVariable String bookingId) {
        return ApiResponse.success(bookingService.resendBookingConfirmation(
                AuthContext.getRequiredUserId(),
                AuthContext.getRequiredUser().getRole(),
                bookingId));
    }

    @GetMapping("/confirmations")
    public ApiResponse<List<Map<String, Object>>> listConfirmations() {
        return ApiResponse.success(bookingService.listConfirmations(AuthContext.getRequiredUserId()));
    }
}
