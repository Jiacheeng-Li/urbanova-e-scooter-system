package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.DiscountRuleService;
import com.lcyhz.urbanova.service.support.PlatformSupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
        return ApiResponse.success(discountRuleService.listPolicies());
    }

    @PostMapping("/admin/discount-rules")
    public ApiResponse<Map<String, Object>> createRule(@RequestBody Map<String, Object> request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        Map<String, Object> result = discountRuleService.createPolicy(request);
        platformSupportService.recordAudit("PROMOTION_POLICY_CREATED", "PROMOTION_POLICY",
                String.valueOf(result.get("promotionPolicyId")), String.valueOf(result.get("policyCode")));
        return ApiResponse.success(result);
    }

    @PatchMapping("/admin/discount-rules/{discountRuleId}")
    public ApiResponse<Map<String, Object>> updateRule(@PathVariable("discountRuleId") String discountRuleId,
                                                       @RequestBody Map<String, Object> request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        Map<String, Object> result = discountRuleService.updatePolicy(requireIdentifier(discountRuleId, "discountRuleId"), request);
        platformSupportService.recordAudit("PROMOTION_POLICY_UPDATED", "PROMOTION_POLICY",
                String.valueOf(result.get("promotionPolicyId")), String.valueOf(result.get("policyCode")));
        return ApiResponse.success(result);
    }

    @GetMapping("/admin/promotion-policies")
    public ApiResponse<List<Map<String, Object>>> listPolicies() {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(discountRuleService.listPolicies());
    }

    @PostMapping("/admin/promotion-policies")
    public ApiResponse<Map<String, Object>> createPolicy(@RequestBody Map<String, Object> request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        Map<String, Object> result = discountRuleService.createPolicy(request);
        platformSupportService.recordAudit("PROMOTION_POLICY_CREATED", "PROMOTION_POLICY",
                String.valueOf(result.get("promotionPolicyId")), String.valueOf(result.get("policyCode")));
        return ApiResponse.success(result);
    }

    @PatchMapping("/admin/promotion-policies/{promotionPolicyId}")
    public ApiResponse<Map<String, Object>> updatePolicy(@PathVariable("promotionPolicyId") String promotionPolicyId,
                                                         @RequestBody Map<String, Object> request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        Map<String, Object> result = discountRuleService.updatePolicy(requireIdentifier(promotionPolicyId, "promotionPolicyId"), request);
        platformSupportService.recordAudit("PROMOTION_POLICY_UPDATED", "PROMOTION_POLICY",
                String.valueOf(result.get("promotionPolicyId")), String.valueOf(result.get("policyCode")));
        return ApiResponse.success(result);
    }

    private String requireIdentifier(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), ErrorCodes.VALIDATION_ERROR,
                    fieldName + " is required");
        }
        return value.trim();
    }
}
