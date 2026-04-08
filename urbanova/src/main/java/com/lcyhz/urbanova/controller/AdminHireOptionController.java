package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.admin.hire.CreateHireOptionRequest;
import com.lcyhz.urbanova.dto.admin.hire.UpdateHireOptionRequest;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.HireOptionService;
import com.lcyhz.urbanova.vo.hire.AdminHireOptionVo;
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
@RequestMapping("/api/v1/admin/hire-options")
@RequiredArgsConstructor
public class AdminHireOptionController {
    private final HireOptionService hireOptionService;

    @GetMapping
    public ApiResponse<List<AdminHireOptionVo>> listHireOptions() {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(hireOptionService.listAllHireOptions());
    }

    @PostMapping
    public ApiResponse<AdminHireOptionVo> createHireOption(@Valid @RequestBody CreateHireOptionRequest request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(hireOptionService.createHireOption(request));
    }

    @PatchMapping("/{hireOptionId}")
    public ApiResponse<AdminHireOptionVo> updateHireOption(@PathVariable String hireOptionId,
                                                           @Valid @RequestBody UpdateHireOptionRequest request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(hireOptionService.updateHireOption(hireOptionId, request));
    }

    @DeleteMapping("/{hireOptionId}")
    public ApiResponse<AdminHireOptionVo> disableHireOption(@PathVariable String hireOptionId) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(hireOptionService.disableHireOption(hireOptionId));
    }
}
