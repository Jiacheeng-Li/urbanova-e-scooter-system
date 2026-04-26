package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.ScooterService;
import com.lcyhz.urbanova.service.UserLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserLocationController {
    private final UserLocationService userLocationService;
    private final ScooterService scooterService;

    @PostMapping("/location")
    public ApiResponse<Map<String, Object>> updateLocation(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(userLocationService.upsertLocation(AuthContext.getRequiredUserId(), request));
    }

    @GetMapping("/map-view")
    public ApiResponse<Map<String, Object>> getMapView() {
        return ApiResponse.success(scooterService.getMapView(AuthContext.getRequiredUserId()));
    }
}
