package com.example.platform.topbiz.config;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.common.error.BusinessException;
import com.example.platform.topbiz.security.TopbizPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class TopbizFeignConfig {

    @Bean
    public RequestInterceptor traceForwardingInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                copyIfPresent(request.getHeader("X-Trace-Id"), "X-Trace-Id", requestTemplate);
                copyIfPresent(request.getHeader("X-Request-Id"), "X-Request-Id", requestTemplate);
            }

            Subject subject = SecurityUtils.getSubject();
            if (subject != null && subject.getPrincipal() instanceof TopbizPrincipal principal) {
                requestTemplate.header("X-User-Id", String.valueOf(principal.userId()));
                requestTemplate.header("X-Session-Key", principal.sessionKey());
            }
        };
    }

    @Bean
    public ErrorDecoder topbizRemoteErrorDecoder(ObjectMapper objectMapper) {
        return (methodKey, response) -> decodeRemoteError(methodKey, response, objectMapper);
    }

    private Exception decodeRemoteError(String methodKey, Response response, ObjectMapper objectMapper) {
        String message = "remote service call failed: " + methodKey;
        String code = "REMOTE_CALL_FAILED";
        try {
            if (response.body() != null) {
                String body = Util.toString(response.body().asReader(StandardCharsets.UTF_8));
                ApiResponse<?> apiResponse = objectMapper.readValue(body, ApiResponse.class);
                if (apiResponse != null) {
                    if (apiResponse.message() != null && !apiResponse.message().isBlank()) {
                        message = apiResponse.message();
                    }
                    if (apiResponse.code() != null && !apiResponse.code().isBlank()) {
                        code = apiResponse.code();
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return new BusinessException(code, message, HttpStatus.valueOf(response.status()));
    }

    private void copyIfPresent(String value, String headerName, feign.RequestTemplate requestTemplate) {
        if (value != null && !value.isBlank()) {
            requestTemplate.header(headerName, value);
        }
    }
}
