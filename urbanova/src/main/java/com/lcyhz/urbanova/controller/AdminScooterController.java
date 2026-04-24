package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.admin.scooter.BulkUpdateScooterStatusRequest;
import com.lcyhz.urbanova.dto.admin.scooter.CreateScooterRequest;
import com.lcyhz.urbanova.dto.admin.scooter.UpdateScooterRequest;
import com.lcyhz.urbanova.dto.admin.scooter.UpdateScooterStatusRequest;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.ScooterService;
import com.lcyhz.urbanova.service.support.PlatformSupportService;
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
    private final PlatformSupportService platformSupportService;

    @GetMapping
    public ApiResponse<List<AdminScooterVo>> listScooters(@RequestParam(required = false) String status) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(scooterService.listAdminScooters(status));
    }

    @PostMapping
    public ApiResponse<AdminScooterVo> createScooter(@Valid @RequestBody CreateScooterRequest request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        AdminScooterVo result = scooterService.createScooter(request);
        platformSupportService.recordAudit("SCOOTER_CREATED", "SCOOTER", result.getScooterId(), result.getStatus());
        return ApiResponse.success(result);
    }

    @PatchMapping("/{scooterId}")
    public ApiResponse<AdminScooterVo> updateScooter(@PathVariable String scooterId,
                                                     @Valid @RequestBody UpdateScooterRequest request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        AdminScooterVo result = scooterService.updateScooter(scooterId, request);
        platformSupportService.recordAudit("SCOOTER_UPDATED", "SCOOTER", result.getScooterId(), result.getStatus());
        return ApiResponse.success(result);
    }

    @PatchMapping("/{scooterId}/status")
    public ApiResponse<AdminScooterVo> updateScooterStatus(@PathVariable String scooterId,
                                                           @Valid @RequestBody UpdateScooterStatusRequest request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        AdminScooterVo result = scooterService.updateScooterStatus(scooterId, request);
        platformSupportService.recordAudit("SCOOTER_STATUS_UPDATED", "SCOOTER", result.getScooterId(), result.getStatus());
        return ApiResponse.success(result);
    }

    @PostMapping("/bulk-status")
    public ApiResponse<BulkScooterStatusUpdateVo> bulkUpdateScooterStatus(
            @Valid @RequestBody BulkUpdateScooterStatusRequest request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        BulkScooterStatusUpdateVo result = scooterService.bulkUpdateScooterStatus(request);
        platformSupportService.recordAudit("SCOOTER_BULK_STATUS_UPDATED", "SCOOTER", String.join(",", result.getScooterIds()), result.getStatus());
        return ApiResponse.success(result);
    }
}
