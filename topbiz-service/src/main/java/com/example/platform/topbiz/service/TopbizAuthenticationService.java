package com.example.platform.topbiz.service;

import com.example.platform.topbiz.dto.LoginResponse;
import com.example.platform.topbiz.security.TopbizPrincipal;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Service;

@Service
public class TopbizAuthenticationService {

    public LoginResponse login(String account, String password) {
        Subject subject = SecurityUtils.getSubject();
        subject.login(new UsernamePasswordToken(account, password));
        TopbizPrincipal principal = (TopbizPrincipal) subject.getPrincipal();
        return new LoginResponse(
                subject.getSession().getId().toString(),
                principal.userId(),
                principal.account(),
                principal.userName(),
                principal.roles(),
                principal.permissions()
        );
    }

    public void logout() {
        SecurityUtils.getSubject().logout();
    }
}
