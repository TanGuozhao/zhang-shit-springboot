package com.example.platform.topbiz.remote.dto;

public record RemoteLogIngestRequest(
        String serviceName,
        String traceId,
        String level,
        String message
) {
}
