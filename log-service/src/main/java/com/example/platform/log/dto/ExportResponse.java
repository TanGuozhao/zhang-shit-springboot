package com.example.platform.log.dto;

public record ExportResponse(
        String exportId,
        String status,
        String downloadPath
) {
}
