package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.service.ScooterTypeService;
import com.lcyhz.urbanova.vo.scooter.ScooterTypeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ScooterTypeController {
    private final ScooterTypeService scooterTypeService;

    @GetMapping("/scooter-types")
    public ApiResponse<List<ScooterTypeVo>> listScooterTypes() {
        return ApiResponse.success(scooterTypeService.listActiveScooterTypes());
    }

    @GetMapping("/scooter-types/{typeCode}")
    public ApiResponse<ScooterTypeVo> getScooterType(@PathVariable String typeCode) {
        return ApiResponse.success(scooterTypeService.getScooterType(typeCode));
    }
}
