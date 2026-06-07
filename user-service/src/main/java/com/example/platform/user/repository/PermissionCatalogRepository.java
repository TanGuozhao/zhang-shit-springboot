package com.example.platform.user.repository;

import com.example.platform.user.domain.PermissionDefinition;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Repository
public class PermissionCatalogRepository {

    private final List<PermissionDefinition> permissions = List.of(
            new PermissionDefinition("user:self:read", "Read Self Profile", "API", "View current user profile"),
            new PermissionDefinition("user:self:write", "Update Self Profile", "API", "Update current user profile"),
            new PermissionDefinition("user:read", "Read Users", "API", "Query other users"),
            new PermissionDefinition("user:write", "Manage Users", "API", "Create and update users"),
            new PermissionDefinition("user:status", "Manage User Status", "API", "Enable, disable or freeze users"),
            new PermissionDefinition("role:read", "Read Roles", "API", "View role catalog"),
            new PermissionDefinition("role:write", "Manage Roles", "API", "Create and update roles"),
            new PermissionDefinition("department:read", "Read Departments", "API", "View departments"),
            new PermissionDefinition("department:write", "Manage Departments", "API", "Create and update departments"),
            new PermissionDefinition("department:tree:read", "Read Organization Tree", "API", "View department hierarchy"),
            new PermissionDefinition("department:attribute:read", "Read Department Attributes", "API", "View department attribute definitions"),
            new PermissionDefinition("department:attribute:write", "Manage Department Attributes", "API", "Create and update department attribute definitions"),
            new PermissionDefinition("department:member:read", "Read Department Members", "API", "View department membership"),
            new PermissionDefinition("department:member:write", "Manage Department Members", "API", "Bind, remove and transfer department members"),
            new PermissionDefinition("message:send", "Send Messages", "API", "Send notifications"),
            new PermissionDefinition("log:query", "Query Logs", "API", "Read access logs"),
            new PermissionDefinition("topbiz:admin", "Administer Topbiz", "API", "Legacy topbiz administrator permission"),
            new PermissionDefinition("topbiz:platform:read", "Read Topbiz Platform", "API", "View topbiz platform overview"),
            new PermissionDefinition("topbiz:architecture:read", "Read Topbiz Architecture", "API", "View topbiz architecture overview"),
            new PermissionDefinition("topbiz:orchestration:write", "Execute Topbiz Orchestration", "API", "Execute topbiz orchestration flows"),
            new PermissionDefinition("topbiz:message:admin", "Administer Topbiz Message", "API", "Manage topbiz message administration features"),
            new PermissionDefinition("topbiz:log:admin", "Administer Topbiz Log", "API", "Manage topbiz log administration features"),
            new PermissionDefinition("topbiz:runtime:operate", "Operate Topbiz Runtime", "API", "Operate topbiz runtime tasks")
    );

    public List<PermissionDefinition> findAll(String permissionType) {
        if (permissionType == null || permissionType.isBlank()) {
            return permissions;
        }
        return permissions.stream()
                .filter(permission -> permission.permissionType().equalsIgnoreCase(permissionType.trim()))
                .toList();
    }

    public boolean existsAll(List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return true;
        }
        Set<String> knownCodes = permissions.stream()
                .map(PermissionDefinition::permissionCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return permissionCodes.stream().allMatch(knownCodes::contains);
    }
}
