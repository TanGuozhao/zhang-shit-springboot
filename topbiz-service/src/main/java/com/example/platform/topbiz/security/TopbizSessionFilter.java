package com.example.platform.topbiz.security;

import com.example.platform.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadState;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class TopbizSessionFilter extends OncePerRequestFilter {

    private static final List<String> ANONYMOUS_PATTERNS = List.of(
            "/actuator/**",
            "/error",
            "/favicon.ico",
            "/api/topbiz/auth/login",
            "/api/topbiz/users/auth/verify-codes",
            "/api/topbiz/users/auth/register",
            "/api/topbiz/users/password/forgot/send-code",
            "/api/topbiz/users/password/forgot/reset",
            "/api/topbiz/users/me/status/unfreeze"
    );

    private final SecurityManager securityManager;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher;

    public TopbizSessionFilter(SecurityManager securityManager, ObjectMapper objectMapper) {
        this.securityManager = securityManager;
        this.objectMapper = objectMapper;
        this.pathMatcher = new AntPathMatcher();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return ANONYMOUS_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Object attribute = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute(TopbizSessionContext.PRINCIPAL_ATTRIBUTE);
        if (!(attribute instanceof TopbizPrincipal principal)) {
            writeUnauthorized(response);
            return;
        }

        Subject subject = new Subject.Builder(securityManager)
                .principals(principal.asPrincipals())
                .authenticated(true)
                .sessionCreationEnabled(false)
                .buildSubject();

        ThreadState threadState = new SubjectThreadState(subject);
        threadState.bind();
        try {
            filterChain.doFilter(request, response);
        } finally {
            threadState.clear();
        }
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail("UNAUTHENTICATED", "login required"));
    }
}
