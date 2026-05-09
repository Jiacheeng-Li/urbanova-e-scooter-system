package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getWallet() {
        return ApiResponse.success(walletService.getWallet(AuthContext.getRequiredUserId()));
    }

    @GetMapping("/transactions")
    public ApiResponse<List<Map<String, Object>>> listTransactions() {
        return ApiResponse.success(walletService.listTransactions(AuthContext.getRequiredUserId()));
    }

    @PostMapping("/top-ups")
    public ApiResponse<Map<String, Object>> topUp(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(walletService.topUp(AuthContext.getRequiredUserId(), request));
    }
}
