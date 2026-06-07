package com.example.platform.topbiz.service;

import com.example.platform.common.api.ApiResponse;
import com.example.platform.common.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RemoteCallSupport {

    public <T> T unwrap(ApiResponse<T> response) {
        if (response == null) {
            throw new BusinessException("REMOTE_EMPTY_RESPONSE", "remote service returned empty response", HttpStatus.BAD_GATEWAY);
        }
        if (!response.success()) {
            throw new BusinessException(response.code(), response.message(), HttpStatus.BAD_GATEWAY);
        }
        return response.data();
    }

    public void ensureOk(ApiResponse<?> response) {
        unwrap(response);
    }
}
