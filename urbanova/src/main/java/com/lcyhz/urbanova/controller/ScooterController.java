package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.service.ScooterService;
import com.lcyhz.urbanova.vo.scooter.ScooterIdsByStatusVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ScooterController {
    private final ScooterService scooterService;

    @GetMapping("/scooters/ids")
    public ApiResponse<ScooterIdsByStatusVo> queryScooterIdsByStatus(@RequestParam String status) {
        return ApiResponse.success(scooterService.queryScooterIdsByStatus(status));
    }
}

