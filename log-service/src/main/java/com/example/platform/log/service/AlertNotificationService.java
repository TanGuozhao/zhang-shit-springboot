package com.example.platform.log.service;

import com.example.platform.log.config.LogServiceProperties;
import com.example.platform.log.domain.AlertEvent;
import com.example.platform.log.domain.AlertRule;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AlertNotificationService {

    private final LogServiceProperties properties;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public AlertNotificationService(LogServiceProperties properties) {
        this.properties = properties;
    }

    public String notifyChannels(AlertRule rule, AlertEvent event) {
        List<String> failures = new ArrayList<>();
        for (String channel : rule.notificationChannels()) {
            String normalized = channel.toUpperCase(Locale.ROOT);
            try {
                if ("CONSOLE".equals(normalized) && properties.notification().consoleEnabled()) {
                    System.out.printf("LOG-ALERT [%s] %s%n", event.alertCode(), event.summary());
                } else if ("WEBHOOK".equals(normalized) && properties.notification().webhookEnabled()) {
                    sendWebhook(rule, event);
                }
            } catch (Exception ex) {
                failures.add(normalized + ":" + ex.getMessage());
            }
        }
        return failures.isEmpty() ? "SUCCESS" : String.join(", ", failures);
    }

    private void sendWebhook(AlertRule rule, AlertEvent event) throws IOException, InterruptedException {
        String url = properties.notification().webhookUrl();
        if (url == null || url.isBlank()) {
            throw new IOException("webhook url is blank");
        }
        String payload = """
                {"ruleId":"%s","ruleName":"%s","alertId":"%s","alertCode":"%s","summary":"%s"}
                """.formatted(rule.ruleId(), rule.ruleName(), event.alertId(), event.alertCode(), event.summary());
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IOException("webhook status " + response.statusCode());
        }
    }
}
