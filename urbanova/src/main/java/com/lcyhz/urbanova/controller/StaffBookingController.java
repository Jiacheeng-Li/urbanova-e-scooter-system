package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff/bookings")
@RequiredArgsConstructor
public class StaffBookingController {
    private final BookingService bookingService;

    @PostMapping("/guest")
    public ApiResponse<Map<String, Object>> createGuestBooking(@RequestBody Map<String, Object> request) {
        AuthContext.requireRole(DomainConstants.ROLE_STAFF);
        return ApiResponse.success(bookingService.createGuestBooking(AuthContext.getRequiredUserId(), request));
    }

    @GetMapping("/guest/{bookingId}")
    public ApiResponse<Map<String, Object>> getGuestBooking(@PathVariable String bookingId) {
        AuthContext.requireAnyRole(DomainConstants.ROLE_STAFF, DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(bookingService.getGuestBookingDetail(AuthContext.getRequiredUserId(), bookingId));
    }
}
