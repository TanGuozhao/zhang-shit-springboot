package com.example.platform.topbiz.security;

public final class TopbizPermissions {

    public static final String USER_SELF_READ = "user:self:read";
    public static final String USER_SELF_WRITE = "user:self:write";
    public static final String USER_READ = "user:read";
    public static final String USER_WRITE = "user:write";
    public static final String USER_STATUS = "user:status";
    public static final String ROLE_READ = "role:read";
    public static final String ROLE_WRITE = "role:write";
    public static final String DEPARTMENT_READ = "department:read";
    public static final String DEPARTMENT_WRITE = "department:write";
    public static final String DEPARTMENT_TREE_READ = "department:tree:read";
    public static final String DEPARTMENT_ATTRIBUTE_READ = "department:attribute:read";
    public static final String DEPARTMENT_ATTRIBUTE_WRITE = "department:attribute:write";
    public static final String DEPARTMENT_MEMBER_READ = "department:member:read";
    public static final String DEPARTMENT_MEMBER_WRITE = "department:member:write";
    public static final String MESSAGE_ACCESS = "message:send";
    public static final String LOG_QUERY = "log:query";
    public static final String TOPBIZ_ADMIN = "topbiz:admin";
    public static final String TOPBIZ_PLATFORM_READ = "topbiz:platform:read";
    public static final String TOPBIZ_ARCHITECTURE_READ = "topbiz:architecture:read";
    public static final String TOPBIZ_ORCHESTRATION_WRITE = "topbiz:orchestration:write";
    public static final String TOPBIZ_MESSAGE_ADMIN = "topbiz:message:admin";
    public static final String TOPBIZ_LOG_ADMIN = "topbiz:log:admin";
    public static final String TOPBIZ_RUNTIME_OPERATE = "topbiz:runtime:operate";

    private TopbizPermissions() {
    }
}
