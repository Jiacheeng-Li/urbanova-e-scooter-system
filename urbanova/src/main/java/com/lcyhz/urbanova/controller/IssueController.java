package com.lcyhz.urbanova.controller;

import com.lcyhz.urbanova.common.api.ApiResponse;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.security.AuthContext;
import com.lcyhz.urbanova.service.IssueManagementService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IssueController {
    private final IssueManagementService issueManagementService;

    @PostMapping("/issues")
    public ApiResponse<Map<String, Object>> createIssue(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(issueManagementService.createIssue(AuthContext.getRequiredUserId(), request));
    }

    @GetMapping("/issues")
    public ApiResponse<List<Map<String, Object>>> listIssues(@RequestParam(required = false) String status) {
        return ApiResponse.success(issueManagementService.listOwnIssues(AuthContext.getRequiredUserId(), status));
    }

    @GetMapping("/issues/{issueId}")
    public ApiResponse<Map<String, Object>> getIssue(@PathVariable String issueId) {
        return ApiResponse.success(issueManagementService.getIssue(
                AuthContext.getRequiredUserId(),
                AuthContext.getRequiredUser().getRole(),
                issueId));
    }

    @PostMapping("/issues/{issueId}/comments")
    public ApiResponse<Map<String, Object>> addComment(@PathVariable String issueId,
                                                       @RequestBody Map<String, Object> request) {
        return ApiResponse.success(issueManagementService.addComment(
                AuthContext.getRequiredUserId(),
                AuthContext.getRequiredUser().getRole(),
                issueId,
                request == null ? null : String.valueOf(request.get("message"))));
    }

    @GetMapping("/admin/issues")
    public ApiResponse<List<Map<String, Object>>> listAdminIssues(@RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) String priority) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(issueManagementService.listAdminIssues(status, priority));
    }

    @PatchMapping("/admin/issues/{issueId}/priority")
    public ApiResponse<Map<String, Object>> updatePriority(@PathVariable String issueId,
                                                           @RequestBody Map<String, Object> request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(issueManagementService.updatePriority(issueId, request == null ? null : String.valueOf(request.get("priority"))));
    }

    @PatchMapping("/admin/issues/{issueId}/status")
    public ApiResponse<Map<String, Object>> updateStatus(@PathVariable String issueId,
                                                         @RequestBody Map<String, Object> request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(issueManagementService.updateStatus(issueId, request == null ? null : String.valueOf(request.get("status"))));
    }

    @PostMapping("/admin/issues/{issueId}/resolve")
    public ApiResponse<Map<String, Object>> resolveIssue(@PathVariable String issueId,
                                                         @RequestBody(required = false) Map<String, Object> request) {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(issueManagementService.resolve(issueId, request == null ? null : String.valueOf(request.get("feedback"))));
    }

    @GetMapping("/admin/issues/high-priority")
    public ApiResponse<List<Map<String, Object>>> listHighPriorityIssues() {
        AuthContext.requireRole(DomainConstants.ROLE_MANAGER);
        return ApiResponse.success(issueManagementService.listHighPriorityIssues());
    }
}
