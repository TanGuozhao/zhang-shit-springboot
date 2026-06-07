package com.example.platform.user.controller;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.user.dto.AdminDepartmentCreateRequest;
import com.example.platform.user.dto.AdminDepartmentSummaryResponse;
import com.example.platform.user.dto.AdminDepartmentUpdateRequest;
import com.example.platform.user.dto.DepartmentAttributeDefinitionCreateRequest;
import com.example.platform.user.dto.DepartmentAttributeDefinitionResponse;
import com.example.platform.user.dto.DepartmentAttributesUpdateRequest;
import com.example.platform.user.dto.DepartmentMemberAttributesBatchRequest;
import com.example.platform.user.dto.DepartmentMemberAttributesBatchResultResponse;
import com.example.platform.user.dto.DepartmentMemberAttributesUpdateRequest;
import com.example.platform.user.dto.DepartmentMemberRelationRequest;
import com.example.platform.user.dto.DepartmentMembershipResponse;
import com.example.platform.user.dto.DepartmentTransferRequest;
import com.example.platform.user.dto.DepartmentUserSummaryResponse;
import com.example.platform.user.dto.OrganizationTreeNodeResponse;
import com.example.platform.user.dto.PagedResult;
import com.example.platform.user.service.DepartmentAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/admin/departments")
public class DepartmentAdminController {

    private final DepartmentAdminService departmentAdminService;

    public DepartmentAdminController(DepartmentAdminService departmentAdminService) {
        this.departmentAdminService = departmentAdminService;
    }

    @GetMapping
    public ApiResponse<PagedResult<AdminDepartmentSummaryResponse>> listDepartments(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @RequestParam(value = "deptName", required = false) String deptName,
            @RequestParam(value = "parentDepartmentId", required = false) Long parentDepartmentId,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.ok(departmentAdminService.listDepartments(
                operatorUserId,
                sessionKey,
                deptName,
                parentDepartmentId,
                pageNum,
                pageSize
        ));
    }

    @GetMapping("/tree")
    public ApiResponse<List<OrganizationTreeNodeResponse>> getOrganizationTree(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey) {
        return ApiResponse.ok(departmentAdminService.getOrganizationTree(operatorUserId, sessionKey));
    }

    @PostMapping
    public ApiResponse<AdminDepartmentSummaryResponse> createDepartment(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @Valid @RequestBody AdminDepartmentCreateRequest request) {
        return ApiResponse.ok(departmentAdminService.createDepartment(operatorUserId, sessionKey, request));
    }

    @PutMapping("/{departmentId}")
    public ApiResponse<AdminDepartmentSummaryResponse> updateDepartment(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @PathVariable Long departmentId,
            @Valid @RequestBody AdminDepartmentUpdateRequest request) {
        return ApiResponse.ok(departmentAdminService.updateDepartment(operatorUserId, sessionKey, departmentId, request));
    }

    @DeleteMapping("/{departmentId}")
    public ApiResponse<Void> deleteDepartment(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @PathVariable Long departmentId) {
        departmentAdminService.deleteDepartment(operatorUserId, sessionKey, departmentId);
        return ApiResponse.ok("department deleted");
    }

    @GetMapping("/{departmentId}/users")
    public ApiResponse<PagedResult<DepartmentUserSummaryResponse>> listDepartmentUsers(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @PathVariable Long departmentId,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.ok(departmentAdminService.listDepartmentUsers(
                operatorUserId,
                sessionKey,
                departmentId,
                account,
                userName,
                pageNum,
                pageSize
        ));
    }

    @GetMapping("/{departmentId}/attributes/definitions")
    public ApiResponse<List<DepartmentAttributeDefinitionResponse>> listAttributeDefinitions(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @PathVariable Long departmentId) {
        return ApiResponse.ok(departmentAdminService.listAttributeDefinitions(operatorUserId, sessionKey, departmentId));
    }

    @PostMapping("/{departmentId}/attributes/definitions")
    public ApiResponse<DepartmentAttributeDefinitionResponse> createAttributeDefinition(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentAttributeDefinitionCreateRequest request) {
        return ApiResponse.ok(departmentAdminService.createAttributeDefinition(
                operatorUserId,
                sessionKey,
                departmentId,
                request
        ));
    }

    @DeleteMapping("/{departmentId}/attributes/definitions/{attributeKey}")
    public ApiResponse<Void> deleteAttributeDefinition(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @PathVariable Long departmentId,
            @PathVariable String attributeKey) {
        departmentAdminService.deleteAttributeDefinition(operatorUserId, sessionKey, departmentId, attributeKey);
        return ApiResponse.ok("attribute definition deleted");
    }

    @PutMapping("/{departmentId}/attributes")
    public ApiResponse<AdminDepartmentSummaryResponse> updateDepartmentAttributes(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentAttributesUpdateRequest request) {
        return ApiResponse.ok(departmentAdminService.updateDepartmentAttributes(
                operatorUserId,
                sessionKey,
                departmentId,
                request
        ));
    }

    @PostMapping("/{departmentId}/members")
    public ApiResponse<PagedResult<DepartmentUserSummaryResponse>> addMembers(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentMemberRelationRequest request) {
        return ApiResponse.ok(departmentAdminService.addMembers(operatorUserId, sessionKey, departmentId, request));
    }

    @DeleteMapping("/{departmentId}/members/{userId}")
    public ApiResponse<DepartmentMembershipResponse> removeMember(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @PathVariable Long departmentId,
            @PathVariable Long userId) {
        return ApiResponse.ok(departmentAdminService.removeMember(operatorUserId, sessionKey, departmentId, userId));
    }

    @PostMapping("/transfer")
    public ApiResponse<List<DepartmentMembershipResponse>> transferMembers(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @Valid @RequestBody DepartmentTransferRequest request) {
        return ApiResponse.ok(departmentAdminService.transferMembers(operatorUserId, sessionKey, request));
    }

    @GetMapping("/users/{userId}/membership")
    public ApiResponse<DepartmentMembershipResponse> getUserMembership(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @PathVariable Long userId) {
        return ApiResponse.ok(departmentAdminService.getUserMembership(operatorUserId, sessionKey, userId));
    }

    @PutMapping("/{departmentId}/members/{userId}/attributes")
    public ApiResponse<DepartmentMembershipResponse> updateMemberAttributes(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @PathVariable Long departmentId,
            @PathVariable Long userId,
            @Valid @RequestBody DepartmentMemberAttributesUpdateRequest request) {
        return ApiResponse.ok(departmentAdminService.updateMemberAttributes(
                operatorUserId,
                sessionKey,
                departmentId,
                userId,
                request
        ));
    }

    @PostMapping("/{departmentId}/members/attributes:batch")
    public ApiResponse<DepartmentMemberAttributesBatchResultResponse> batchUpdateMemberAttributes(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorUserId,
            @RequestHeader(value = "X-Session-Key", required = false) String sessionKey,
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentMemberAttributesBatchRequest request) {
        return ApiResponse.ok(departmentAdminService.batchUpdateMemberAttributes(
                operatorUserId,
                sessionKey,
                departmentId,
                request
        ));
    }
}
