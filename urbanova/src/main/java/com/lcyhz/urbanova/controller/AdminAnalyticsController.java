package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/revenue/estimate")
    public ApiResponse<Map<String, Object>> revenueEstimate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(analyticsService.revenueEstimate(startDate, endDate));
    }

    @GetMapping("/revenue/weekly-by-hire-option")
    public ApiResponse<List<Map<String, Object>>> weeklyByHireOption(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(analyticsService.weeklyByHireOption(startDate));
    }

    @GetMapping("/revenue/daily-combined")
    public ApiResponse<List<Map<String, Object>>> dailyCombined(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(analyticsService.dailyCombined(startDate));
    }

    @GetMapping("/revenue/weekly-chart")
    public ApiResponse<Map<String, Object>> weeklyChart(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(analyticsService.weeklyChart(startDate));
    }

    @GetMapping("/usage/frequent-users")
    public ApiResponse<List<Map<String, Object>>> frequentUsers() {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(analyticsService.frequentUsers());
    }
}
