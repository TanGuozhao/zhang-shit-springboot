package com.example.platform.user.repository;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserModuleRepository {

    public List<String> coreModules() {
        return List.of("user", "role", "permission", "department", "account");
    }

    public List<String> layers() {
        return List.of("controller", "dto", "service", "repository", "config", "domain");
    }
}
