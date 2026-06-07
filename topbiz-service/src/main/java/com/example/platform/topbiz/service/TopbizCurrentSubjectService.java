package com.example.platform.topbiz.service;

import com.example.platform.common.error.BusinessException;
import com.example.platform.topbiz.security.TopbizPrincipal;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TopbizCurrentSubjectService {

    public TopbizPrincipal requirePrincipal() {
        Subject subject = SecurityUtils.getSubject();
        if (subject == null || !(subject.getPrincipal() instanceof TopbizPrincipal principal)) {
            throw new BusinessException("UNAUTHENTICATED", "login required", HttpStatus.UNAUTHORIZED);
        }
        return principal;
    }
}
