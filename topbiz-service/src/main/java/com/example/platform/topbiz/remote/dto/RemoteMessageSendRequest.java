package com.example.platform.topbiz.remote.dto;

import java.util.List;
import java.util.Map;

public record RemoteMessageSendRequest(
        String templateCode,
        String channel,
        List<String> receivers,
        Map<String, String> variables
) {
}
