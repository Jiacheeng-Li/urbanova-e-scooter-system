package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.dto.auth.LoginRequest;
import com.lcyhz.urbanova.dto.auth.RegisterRequest;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.AuthService;
import com.lcyhz.urbanova.service.UserManagementService;
import com.lcyhz.urbanova.vo.auth.AuthPayload;
import com.lcyhz.urbanova.vo.auth.UserProfileVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserManagementService userManagementService;

    @PostMapping("/auth/register")
    public ApiResponse<AuthPayload> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/auth/login")
    public ApiResponse<AuthPayload> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<AuthPayload> refresh(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(authService.refresh(request == null ? null : (String) request.get("refreshToken")));
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Map<String, Object>> logout(@RequestBody(required = false) Map<String, Object> request) {
        String refreshToken = request == null ? null : (String) request.get("refreshToken");
        return ApiResponse.success(authService.logout(AuthContext.getRequiredUserId(), refreshToken));
    }

    @PostMapping("/auth/password/forgot")
    public ApiResponse<Map<String, Object>> forgotPassword(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(authService.forgotPassword(request == null ? null : (String) request.get("email")));
    }

    @PostMapping("/auth/password/reset")
    public ApiResponse<Map<String, Object>> resetPassword(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(authService.resetPassword(
                request == null ? null : (String) request.get("resetToken"),
                request == null ? null : (String) request.get("newPassword")));
    }

    @GetMapping("/users/me")
    public ApiResponse<UserProfileVo> me() {
        return ApiResponse.success(authService.getCurrentUser(AuthContext.getRequiredUserId()));
    }

    @PatchMapping("/users/me")
    public ApiResponse<Map<String, Object>> updateMe(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(userManagementService.updateCurrentUser(AuthContext.getRequiredUserId(), request));
    }

    @GetMapping("/users/me/usage-summary")
    public ApiResponse<Map<String, Object>> usageSummary() {
        return ApiResponse.success(userManagementService.getUsageSummary(AuthContext.getRequiredUserId()));
    }
}

