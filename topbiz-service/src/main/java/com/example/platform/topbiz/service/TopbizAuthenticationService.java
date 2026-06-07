package com.example.platform.topbiz.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.topbiz.dto.LoginResponse;
import com.example.platform.topbiz.remote.UserServiceClient;
import com.example.platform.topbiz.security.TopbizPrincipal;
import com.example.platform.topbiz.security.TopbizSessionContext;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class TopbizAuthenticationService {

    private final SecurityManager securityManager;
    private final UserServiceClient userServiceClient;

    public TopbizAuthenticationService(SecurityManager securityManager,
                                       UserServiceClient userServiceClient) {
        this.securityManager = securityManager;
        this.userServiceClient = userServiceClient;
    }

    public LoginResponse login(String account, String password) {
        Subject subject = new Subject.Builder(securityManager).buildSubject();
        subject.login(new UsernamePasswordToken(account, password));
        TopbizPrincipal principal = (TopbizPrincipal) subject.getPrincipal();

        HttpSession session = currentRequest().getSession(true);
        session.setAttribute(TopbizSessionContext.PRINCIPAL_ATTRIBUTE, principal);
        return toLoginResponse(session.getId(), principal);
    }

    public LoginResponse currentSession() {
        HttpServletRequest request = currentRequest();
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new BusinessException("UNAUTHENTICATED", "login required", HttpStatus.UNAUTHORIZED);
        }
        Object attribute = session.getAttribute(TopbizSessionContext.PRINCIPAL_ATTRIBUTE);
        if (!(attribute instanceof TopbizPrincipal principal)) {
            throw new BusinessException("UNAUTHENTICATED", "login required", HttpStatus.UNAUTHORIZED);
        }
        return toLoginResponse(session.getId(), principal);
    }

    public void logout() {
        HttpServletRequest request = currentRequest();
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object attribute = session.getAttribute(TopbizSessionContext.PRINCIPAL_ATTRIBUTE);
            if (attribute instanceof TopbizPrincipal principal) {
                userServiceClient.logout(principal.sessionKey());
            }
            session.invalidate();
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException("REQUEST_CONTEXT_MISSING", "request context is not available", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return attributes.getRequest();
    }

    private LoginResponse toLoginResponse(String sessionId, TopbizPrincipal principal) {
        return new LoginResponse(
                sessionId,
                principal.sessionKey(),
                principal.expireTime(),
                principal.userId(),
                principal.account(),
                principal.userName(),
                principal.roles(),
                principal.permissions()
        );
    }
}
