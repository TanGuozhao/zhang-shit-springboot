package com.example.platform.topbiz.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class TopbizFeignConfig {

    @Bean
    public RequestInterceptor traceForwardingInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            String traceId = request.getHeader("X-Trace-Id");
            String userId = request.getHeader("X-User-Id");
            if (traceId != null) {
                requestTemplate.header("X-Trace-Id", traceId);
            }
            if (userId != null) {
                requestTemplate.header("X-User-Id", userId);
            }
        };
    }
}
