package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.service.ScooterService;
import com.lcyhz.urbanova.vo.scooter.ScooterMapPointVo;
import com.lcyhz.urbanova.vo.scooter.ScooterIdsByStatusVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ScooterController {
    private final ScooterService scooterService;

    @GetMapping("/scooters")
    public ApiResponse<List<Map<String, Object>>> listScooters(@RequestParam(required = false) String status,
                                                               @RequestParam(required = false) String typeCode,
                                                               @RequestParam(required = false) String zone) {
        return ApiResponse.success(scooterService.listPublicScooters(status, typeCode, zone));
    }

    @GetMapping("/scooters/availability")
    public ApiResponse<Map<String, Object>> getAvailabilitySummary() {
        return ApiResponse.success(scooterService.getAvailabilitySummary());
    }

    @GetMapping("/scooters/ids")
    public ApiResponse<ScooterIdsByStatusVo> queryScooterIdsByStatus(@RequestParam String status) {
        return ApiResponse.success(scooterService.queryScooterIdsByStatus(status));
    }

    @GetMapping("/scooters/map-points")
    public ApiResponse<List<ScooterMapPointVo>> listScooterMapPoints() {
        return ApiResponse.success(scooterService.listMapPoints());
    }

    @GetMapping("/scooters/{scooterId}")
    public ApiResponse<Map<String, Object>> getScooterDetail(@PathVariable String scooterId) {
        return ApiResponse.success(scooterService.getScooterDetail(scooterId));
    }
}
