package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listNotifications() {
        return ApiResponse.success(notificationService.listNotifications(AuthContext.getRequiredUserId()));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Map<String, Object>> markRead(@PathVariable String notificationId) {
        return ApiResponse.success(notificationService.markRead(AuthContext.getRequiredUserId(), notificationId));
    }
}
