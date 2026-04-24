package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.UserManagementService;
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
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserManagementService userManagementService;

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> listUsers(@RequestParam(required = false) String role,
                                                            @RequestParam(required = false) String accountStatus) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(userManagementService.listUsers(role, accountStatus));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<Map<String, Object>> getUser(@PathVariable String userId) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(userManagementService.getUserDetail(userId));
    }

    @PatchMapping("/users/{userId}/status")
    public ApiResponse<Map<String, Object>> updateStatus(@PathVariable String userId,
                                                         @RequestBody Map<String, Object> request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(userManagementService.updateUserStatus(userId, request == null ? null : String.valueOf(request.get("accountStatus"))));
    }

    @GetMapping("/users/{userId}/bookings")
    public ApiResponse<List<Map<String, Object>>> listUserBookings(@PathVariable String userId) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(userManagementService.listUserBookings(userId));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<Map<String, Object>>> listAuditLogs(@RequestParam(required = false) String action,
                                                                @RequestParam(required = false) Integer limit) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(userManagementService.listAuditLogs(action, limit));
    }
}
