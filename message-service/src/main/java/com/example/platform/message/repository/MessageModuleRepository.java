package com.example.platform.message.repository;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MessageModuleRepository {

    public List<String> coreModules() {
        return List.of("template", "variable", "dispatch", "channel", "inbox");
    }

    public List<String> layers() {
        return List.of("controller", "dto", "service", "repository", "config", "domain");
    }
}
