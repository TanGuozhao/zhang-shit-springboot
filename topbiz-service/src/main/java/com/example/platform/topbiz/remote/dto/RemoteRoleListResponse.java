package com.example.platform.topbiz.remote.dto;

import java.util.List;

public record RemoteRoleListResponse(Long userId, List<String> roles) {
}
