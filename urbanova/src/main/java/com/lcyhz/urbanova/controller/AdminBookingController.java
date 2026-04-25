package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {
    private final BookingService bookingService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listBookings(@RequestParam(required = false) String status,
                                                               @RequestParam(required = false) String paymentStatus,
                                                               @RequestParam(required = false) String customerType) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(bookingService.listAdminBookings(status, paymentStatus, customerType));
    }

    @GetMapping("/{bookingId}")
    public ApiResponse<Map<String, Object>> getBooking(@PathVariable String bookingId) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(bookingService.getAdminBookingDetail(bookingId));
    }

    @PatchMapping("/{bookingId}/override")
    public ApiResponse<Map<String, Object>> overrideBooking(@PathVariable String bookingId,
                                                            @RequestBody Map<String, Object> request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(bookingService.overrideBooking(bookingId, request));
    }
}
