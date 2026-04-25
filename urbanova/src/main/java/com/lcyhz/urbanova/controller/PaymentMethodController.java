package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {
    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listPaymentMethods() {
        return ApiResponse.success(paymentMethodService.listPaymentMethods(AuthContext.getRequiredUserId()));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createPaymentMethod(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(paymentMethodService.createPaymentMethod(AuthContext.getRequiredUserId(), request));
    }

    @PatchMapping("/{paymentMethodId}")
    public ApiResponse<Map<String, Object>> updatePaymentMethod(@PathVariable String paymentMethodId,
                                                                @RequestBody Map<String, Object> request) {
        return ApiResponse.success(paymentMethodService.updatePaymentMethod(AuthContext.getRequiredUserId(), paymentMethodId, request));
    }

    @DeleteMapping("/{paymentMethodId}")
    public ApiResponse<Map<String, Object>> deletePaymentMethod(@PathVariable String paymentMethodId) {
        return ApiResponse.success(paymentMethodService.deletePaymentMethod(AuthContext.getRequiredUserId(), paymentMethodId));
    }

    @PostMapping("/{paymentMethodId}/default")
    public ApiResponse<Map<String, Object>> setDefault(@PathVariable String paymentMethodId) {
        return ApiResponse.success(paymentMethodService.setDefault(AuthContext.getRequiredUserId(), paymentMethodId));
    }
}
