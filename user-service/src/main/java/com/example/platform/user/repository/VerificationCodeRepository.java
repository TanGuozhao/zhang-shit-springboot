package com.example.platform.user.repository;

import com.example.platform.user.domain.VerificationCode;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class VerificationCodeRepository {

    private static final String DEFAULT_VERIFY_CODE = "123456";

    private final ConcurrentMap<String, VerificationCode> codes = new ConcurrentHashMap<>();

    public VerificationCode save(String account, String contact, String scene, Duration ttl) {
        Instant issuedAt = Instant.now();
        VerificationCode code = new VerificationCode(
                account.trim(),
                contact.trim(),
                normalizeScene(scene),
                DEFAULT_VERIFY_CODE,
                issuedAt,
                issuedAt.plus(ttl)
        );
        codes.put(key(account, scene), code);
        return code;
    }

    public boolean verify(String account, String contact, String scene, String code) {
        VerificationCode saved = codes.get(key(account, scene));
        if (saved == null) {
            return false;
        }
        return saved.contact().equalsIgnoreCase(contact.trim())
                && saved.code().equals(code)
                && saved.expiresAt().isAfter(Instant.now());
    }

    public Optional<VerificationCode> find(String account, String scene) {
        return Optional.ofNullable(codes.get(key(account, scene)));
    }

    public void remove(String account, String scene) {
        codes.remove(key(account, scene));
    }

    private String key(String account, String scene) {
        return account.trim().toLowerCase(Locale.ROOT) + "::" + normalizeScene(scene);
    }

    private String normalizeScene(String scene) {
        return scene == null ? "DEFAULT" : scene.trim().toUpperCase(Locale.ROOT);
    }
}
