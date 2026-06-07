package com.example.platform.user.dto;

import java.util.List;

public record DepartmentMemberAttributesBatchResultResponse(
        boolean allSucceeded,
        List<DepartmentMemberAttributesBatchResultItemResponse> results
) {
}
