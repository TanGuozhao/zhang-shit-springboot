package com.example.platform.topbiz.service;

import com.example.platform.common.web.TraceIdFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TopbizTraceSupport {

    public String currentTraceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return traceId;
    }
}
