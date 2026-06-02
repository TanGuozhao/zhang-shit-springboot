package com.example.platform.log.dto;

import jakarta.validation.constraints.NotBlank;

public record ExportRequest(
        @NotBlank(message = "format is required") String format,
        String query
) {
}
