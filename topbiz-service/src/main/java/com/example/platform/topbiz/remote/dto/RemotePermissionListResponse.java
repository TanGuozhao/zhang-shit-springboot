package com.example.platform.topbiz.remote.dto;

import java.util.List;

public record RemotePermissionListResponse(Long userId, List<String> permissions) {
}
