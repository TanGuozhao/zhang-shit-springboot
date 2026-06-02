package com.example.platform.log.repository;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LogModuleRepository {

    public List<String> coreModules() {
        return List.of("ingest", "search", "metrics", "alert", "export");
    }

    public List<String> layers() {
        return List.of("controller", "dto", "service", "repository", "config", "domain");
    }
}
