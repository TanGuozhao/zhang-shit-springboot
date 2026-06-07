package com.example.platform.message.repository;

import com.example.platform.message.domain.MessageChannel;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ChannelRepository {

    private final Map<String, MessageChannel> channels = new LinkedHashMap<>();

    public ChannelRepository() {
        save(new MessageChannel("EMAIL", "EMAIL", "SendGrid", "email-main", "noreply@example.com", true, true, "Default email channel"));
        save(new MessageChannel("SMS", "SMS", "Aliyun SMS", "sms-primary", "[Platform Notice]", true, true, "Default sms channel"));
        save(new MessageChannel("FEISHU", "BOT", "Feishu Bot", "feishu-robot", "Ops Robot", true, true, "Feishu bot channel"));
        save(new MessageChannel("INBOX", "IN_APP", "Platform Inbox", "inbox-default", "Inbox Center", true, true, "In-app inbox channel"));
    }

    public List<MessageChannel> findAll() {
        return new ArrayList<>(channels.values());
    }

    public Optional<MessageChannel> findByCode(String channelCode) {
        return Optional.ofNullable(channels.get(channelCode.toUpperCase()));
    }

    public void save(MessageChannel channel) {
        channels.put(channel.channelCode().toUpperCase(), channel);
    }
}
