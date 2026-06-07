package com.example.platform.user.dto;

import java.util.List;
import java.util.Map;

public record OrganizationTreeNodeResponse(
        Long departmentId,
        String departmentCode,
        String departmentName,
        Long parentDepartmentId,
        String description,
        int memberCount,
        Map<String, String> attributes,
        List<DepartmentAttributeDefinitionResponse> attributeDefinitions,
        List<OrganizationTreeNodeResponse> children
) {
}
