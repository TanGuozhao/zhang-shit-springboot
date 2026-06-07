package com.example.platform.message.domain;

import java.util.List;

public record ReceiverGroup(
        String groupCode,
        String groupName,
        List<String> receivers,
        boolean enabled
) {
}
