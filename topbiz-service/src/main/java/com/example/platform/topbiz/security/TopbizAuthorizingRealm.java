package com.example.platform.topbiz.security;

import com.example.platform.topbiz.remote.UserServiceClient;
import com.example.platform.topbiz.remote.dto.RemoteAuthLoginRequest;
import com.example.platform.topbiz.remote.dto.RemoteAuthLoginResponse;
import com.example.platform.topbiz.remote.dto.RemotePermissionListResponse;
import com.example.platform.topbiz.remote.dto.RemoteRoleListResponse;
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

    private final UserServiceClient userServiceClient;

    public TopbizAuthorizingRealm(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
        setAuthenticationTokenClass(UsernamePasswordToken.class);
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        TopbizPrincipal principal = (TopbizPrincipal) principals.getPrimaryPrincipal();
        RemoteRoleListResponse roles = userServiceClient.getRoles(principal.userId()).data();
        RemotePermissionListResponse permissions = userServiceClient.getPermissions(principal.userId()).data();
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
                        new String(usernamePasswordToken.getPassword())
                )
        ).data();
        TopbizPrincipal principal = new TopbizPrincipal(
                loginResponse.userId(),
                loginResponse.account(),
                loginResponse.userName(),
                loginResponse.roles(),
                loginResponse.permissions()
        );
        return new SimpleAuthenticationInfo(principal, usernamePasswordToken.getPassword(), getName());
    }
}
