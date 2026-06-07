package com.example.platform.user.dto;

import java.util.List;

public record PagedResult<T>(
        long total,
        List<T> list
) {
}
