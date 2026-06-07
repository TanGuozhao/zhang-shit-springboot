package com.example.platform.topbiz.security;

import com.example.platform.topbiz.remote.UserServiceClient;
import com.example.platform.topbiz.remote.dto.RemoteAuthLoginRequest;
import com.example.platform.topbiz.remote.dto.RemoteAuthLoginResponse;
import com.example.platform.user.dto.PermissionListResponse;
import com.example.platform.user.dto.RoleListResponse;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.stereotype.Component;

@Component
public class TopbizAuthorizingRealm extends AuthorizingRealm {

    public static final String REALM_NAME = "topbizRealm";

    private final UserServiceClient userServiceClient;

    public TopbizAuthorizingRealm(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
        setAuthenticationTokenClass(UsernamePasswordToken.class);
        setName(REALM_NAME);
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        TopbizPrincipal principal = (TopbizPrincipal) principals.getPrimaryPrincipal();
        RoleListResponse roles = userServiceClient.getRoles(principal.userId()).data();
        PermissionListResponse permissions = userServiceClient.getPermissions(principal.userId()).data();
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.addRoles(roles.roles());
        info.addStringPermissions(permissions.permissions());
        return info;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) token;
        RemoteAuthLoginResponse loginResponse = userServiceClient.login(
                new RemoteAuthLoginRequest(
                        usernamePasswordToken.getUsername(),
                        new String(usernamePasswordToken.getPassword()),
                        "password",
                        Boolean.FALSE,
                        null
                )
        ).data();
        TopbizPrincipal principal = new TopbizPrincipal(
                loginResponse.userId(),
                loginResponse.account(),
                loginResponse.userName(),
                loginResponse.roles(),
                loginResponse.permissions(),
                loginResponse.sessionKey(),
                loginResponse.expireTime()
        );
        return new SimpleAuthenticationInfo(principal, usernamePasswordToken.getPassword(), getName());
    }
}
