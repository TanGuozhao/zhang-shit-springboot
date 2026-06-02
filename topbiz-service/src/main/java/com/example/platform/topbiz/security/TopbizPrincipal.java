package com.example.platform.topbiz.security;

import java.io.Serializable;
import java.util.List;

public record TopbizPrincipal(
        Long userId,
        String account,
        String userName,
        List<String> roles,
        List<String> permissions
) implements Serializable {
}
