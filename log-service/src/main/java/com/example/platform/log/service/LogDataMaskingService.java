package com.example.platform.log.service;

import com.example.platform.log.config.LogServiceProperties;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class LogDataMaskingService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(1\\d{2})\\d{4}(\\d{4})(?!\\d)");
    private static final Pattern SECRET_PATTERN = Pattern.compile("(?i)(token|password|secret)(\\s*[=:]\\s*)([^\\s,;]+)");

    private final LogServiceProperties properties;

    public LogDataMaskingService(LogServiceProperties properties) {
        this.properties = properties;
    }

    public String maskText(String value) {
        if (!properties.masking().enabled() || value == null || value.isBlank()) {
            return value;
        }
        String masked = PHONE_PATTERN.matcher(value).replaceAll("$1****$2");
        return SECRET_PATTERN.matcher(masked).replaceAll("$1$2***");
    }
}
