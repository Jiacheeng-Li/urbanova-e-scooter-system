package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.DiscountRuleService;
import com.lcyhz.urbanova.service.support.PlatformSupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DiscountController {
    private final DiscountRuleService discountRuleService;
    private final PlatformSupportService platformSupportService;

    @GetMapping("/discounts/eligibility")
    public ApiResponse<Map<String, Object>> getEligibility() {
        return ApiResponse.success(discountRuleService.getEligibility(AuthContext.getRequiredUserId()));
    }

    @GetMapping("/admin/discount-rules")
    public ApiResponse<List<Map<String, Object>>> listRules() {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(discountRuleService.listRules());
    }

    @PostMapping("/admin/discount-rules")
    public ApiResponse<Map<String, Object>> createRule(@RequestBody Map<String, Object> request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        Map<String, Object> result = discountRuleService.createRule(request);
        platformSupportService.recordAudit("DISCOUNT_RULE_CREATED", "DISCOUNT_RULE", String.valueOf(result.get("discountRuleId")), String.valueOf(result.get("type")));
        return ApiResponse.success(result);
    }

    @PatchMapping("/admin/discount-rules/{discountRuleId}")
    public ApiResponse<Map<String, Object>> updateRule(@PathVariable String discountRuleId,
                                                       @RequestBody Map<String, Object> request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        Map<String, Object> result = discountRuleService.updateRule(discountRuleId, request);
        platformSupportService.recordAudit("DISCOUNT_RULE_UPDATED", "DISCOUNT_RULE", String.valueOf(result.get("discountRuleId")), String.valueOf(result.get("type")));
        return ApiResponse.success(result);
    }
}
