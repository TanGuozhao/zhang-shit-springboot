package com.example.platform.message.repository;

import com.example.platform.message.domain.CarrierAccount;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CarrierAccountRepository {

    private final Map<String, CarrierAccount> accounts = new LinkedHashMap<>();

    public CarrierAccountRepository() {
        save(new CarrierAccount("email-main", "SendGrid", "EMAIL", "demo-email-key", "https://api.sendgrid.local", "n/a", true));
        save(new CarrierAccount("sms-primary", "Aliyun SMS", "SMS", "demo-sms-key", "https://api.sms.local", "[Platform Notice]", true));
        save(new CarrierAccount("feishu-robot", "Feishu Bot", "BOT", "demo-feishu-key", "https://api.feishu.local", "robot", true));
        save(new CarrierAccount("inbox-default", "Platform Inbox", "IN_APP", null, null, "inbox", true));
    }

    public void save(CarrierAccount account) {
        accounts.put(account.accountCode(), account);
    }

    public Optional<CarrierAccount> findByCode(String accountCode) {
        return Optional.ofNullable(accounts.get(accountCode));
    }

    public List<CarrierAccount> findAll() {
        return new ArrayList<>(accounts.values());
    }
}
