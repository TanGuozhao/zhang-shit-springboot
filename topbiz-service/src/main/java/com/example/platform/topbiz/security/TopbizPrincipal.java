package com.example.platform.topbiz.security;

import org.apache.shiro.subject.SimplePrincipalCollection;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public record TopbizPrincipal(
        Long userId,
        String account,
        String userName,
        List<String> roles,
        List<String> permissions,
        String sessionKey,
        Instant expireTime
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public SimplePrincipalCollection asPrincipals() {
        return new SimplePrincipalCollection(this, TopbizAuthorizingRealm.REALM_NAME);
    }
}
