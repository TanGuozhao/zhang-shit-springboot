package com.example.platform.topbiz.remote.dto;

import java.util.Map;

public record RemoteMessageDraftRequest(
        String templateCode,
        String title,
        String content,
        Map<String, String> variables
) {
}
