package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.admin.scooter.BulkUpdateScooterStatusRequest;
import com.lcyhz.urbanova.dto.admin.scooter.CreateScooterRequest;
import com.lcyhz.urbanova.dto.admin.scooter.UpdateScooterRequest;
import com.lcyhz.urbanova.dto.admin.scooter.UpdateScooterStatusRequest;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.ScooterService;
import com.lcyhz.urbanova.vo.scooter.AdminScooterVo;
import com.lcyhz.urbanova.vo.scooter.BulkScooterStatusUpdateVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/scooters")
@RequiredArgsConstructor
public class AdminScooterController {
    private final ScooterService scooterService;

    @GetMapping
    public ApiResponse<List<AdminScooterVo>> listScooters(@RequestParam(required = false) String status) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(scooterService.listAdminScooters(status));
    }

    @PostMapping
    public ApiResponse<AdminScooterVo> createScooter(@Valid @RequestBody CreateScooterRequest request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(scooterService.createScooter(request));
    }

    @PatchMapping("/{scooterId}")
    public ApiResponse<AdminScooterVo> updateScooter(@PathVariable String scooterId,
                                                     @Valid @RequestBody UpdateScooterRequest request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(scooterService.updateScooter(scooterId, request));
    }

    @PatchMapping("/{scooterId}/status")
    public ApiResponse<AdminScooterVo> updateScooterStatus(@PathVariable String scooterId,
                                                           @Valid @RequestBody UpdateScooterStatusRequest request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(scooterService.updateScooterStatus(scooterId, request));
    }

    @PostMapping("/bulk-status")
    public ApiResponse<BulkScooterStatusUpdateVo> bulkUpdateScooterStatus(
            @Valid @RequestBody BulkUpdateScooterStatusRequest request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(scooterService.bulkUpdateScooterStatus(request));
    }
}
