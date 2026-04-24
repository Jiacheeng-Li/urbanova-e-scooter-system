package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.admin.scootertype.CreateScooterTypeRequest;
import com.lcyhz.urbanova.dto.admin.scootertype.UpdateScooterTypeRequest;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.ScooterTypeService;
import com.lcyhz.urbanova.service.support.PlatformSupportService;
import com.lcyhz.urbanova.vo.scooter.AdminScooterTypeVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/scooter-types")
@RequiredArgsConstructor
public class AdminScooterTypeController {
    private final ScooterTypeService scooterTypeService;
    private final PlatformSupportService platformSupportService;

    @GetMapping
    public ApiResponse<List<AdminScooterTypeVo>> listScooterTypes() {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(scooterTypeService.listAllScooterTypes());
    }

    @PostMapping
    public ApiResponse<AdminScooterTypeVo> createScooterType(@Valid @RequestBody CreateScooterTypeRequest request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        AdminScooterTypeVo result = scooterTypeService.createScooterType(request);
        platformSupportService.recordAudit("SCOOTER_TYPE_CREATED", "SCOOTER_TYPE", result.getTypeCode(), result.getDisplayName());
        return ApiResponse.success(result);
    }

    @PatchMapping("/{typeCode}")
    public ApiResponse<AdminScooterTypeVo> updateScooterType(@PathVariable String typeCode,
                                                             @Valid @RequestBody UpdateScooterTypeRequest request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        AdminScooterTypeVo result = scooterTypeService.updateScooterType(typeCode, request);
        platformSupportService.recordAudit("SCOOTER_TYPE_UPDATED", "SCOOTER_TYPE", result.getTypeCode(), result.getDisplayName());
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{typeCode}")
    public ApiResponse<AdminScooterTypeVo> disableScooterType(@PathVariable String typeCode) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        AdminScooterTypeVo result = scooterTypeService.disableScooterType(typeCode);
        platformSupportService.recordAudit("SCOOTER_TYPE_DISABLED", "SCOOTER_TYPE", result.getTypeCode(), result.getDisplayName());
        return ApiResponse.success(result);
    }
}
