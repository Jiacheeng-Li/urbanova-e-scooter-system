package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/bookings/{bookingId}/payments")
    public ApiResponse<Map<String, Object>> createPayment(@PathVariable String bookingId,
                                                          @RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(paymentService.createPayment(
                AuthContext.getRequiredUserId(),
                AuthContext.getRequiredUser().getRole(),
                bookingId,
                request));
    }

    @GetMapping("/bookings/{bookingId}/payments")
    public ApiResponse<List<Map<String, Object>>> listBookingPayments(@PathVariable String bookingId) {
        return ApiResponse.success(paymentService.listBookingPayments(
                AuthContext.getRequiredUserId(),
                AuthContext.getRequiredUser().getRole(),
                bookingId));
    }

    @GetMapping("/payments/{paymentId}")
    public ApiResponse<Map<String, Object>> getPaymentDetail(@PathVariable String paymentId) {
        return ApiResponse.success(paymentService.getPaymentDetail(
                AuthContext.getRequiredUserId(),
                AuthContext.getRequiredUser().getRole(),
                paymentId));
    }

    @PostMapping("/payments/{paymentId}/simulate-settlement")
    public ApiResponse<Map<String, Object>> simulateSettlement(@PathVariable String paymentId,
                                                               @RequestBody(required = false) Map<String, Object> request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(paymentService.simulateSettlement(
                paymentId,
                request == null ? null : String.valueOf(request.get("simulatedOutcome")),
                AuthContext.getRequiredUserId(),
                AuthContext.getRequiredUser().getRole()));
    }

    @PostMapping("/payments/{paymentId}/refund")
    public ApiResponse<Map<String, Object>> refundPayment(@PathVariable String paymentId,
                                                          @RequestBody(required = false) Map<String, Object> request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(paymentService.refundPayment(paymentId, request));
    }
}
