package com.example.platform.user.repository;

import com.example.platform.user.domain.ExternalIdentityBinding;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class ExternalIdentityBindingRepository {

    private final AtomicLong bindingIdSequence = new AtomicLong(0L);
    private final ConcurrentMap<Long, ExternalIdentityBinding> bindings = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> providerUserIndex = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> providerUnionIndex = new ConcurrentHashMap<>();

    public Optional<ExternalIdentityBinding> findByProviderAndSubject(String provider,
                                                                      String providerUserId,
                                                                      String providerUnionId) {
        String normalizedProvider = normalizeProvider(provider);
        if (providerUnionId != null && !providerUnionId.isBlank()) {
            Long bindingId = providerUnionIndex.get(unionKey(normalizedProvider, providerUnionId));
            if (bindingId != null) {
                return Optional.ofNullable(bindings.get(bindingId));
            }
        }
        if (providerUserId == null || providerUserId.isBlank()) {
            return Optional.empty();
        }
        Long bindingId = providerUserIndex.get(userKey(normalizedProvider, providerUserId));
        if (bindingId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(bindings.get(bindingId));
    }

    public ExternalIdentityBinding saveOrUpdate(String provider,
                                                String providerUserId,
                                                String providerUnionId,
                                                Long userId,
                                                String accountSnapshot,
                                                String userNameSnapshot,
                                                String emailSnapshot,
                                                String avatarSnapshot,
                                                Map<String, String> rawProfile) {
        String normalizedProvider = normalizeProvider(provider);
        String normalizedUserId = normalizeValue(providerUserId);
        String normalizedUnionId = normalizeValue(providerUnionId);
        Instant now = Instant.now();

        Optional<ExternalIdentityBinding> existing = findByProviderAndSubject(
                normalizedProvider,
                normalizedUserId,
                normalizedUnionId
        );

        ExternalIdentityBinding binding = existing
                .map(current -> new ExternalIdentityBinding(
                        current.bindingId(),
                        normalizedProvider,
                        normalizedUserId,
                        normalizedUnionId,
                        userId,
                        accountSnapshot,
                        userNameSnapshot,
                        emailSnapshot,
                        avatarSnapshot,
                        copyMap(rawProfile),
                        current.createdAt(),
                        now
                ))
                .orElseGet(() -> new ExternalIdentityBinding(
                        bindingIdSequence.incrementAndGet(),
                        normalizedProvider,
                        normalizedUserId,
                        normalizedUnionId,
                        userId,
                        accountSnapshot,
                        userNameSnapshot,
                        emailSnapshot,
                        avatarSnapshot,
                        copyMap(rawProfile),
                        now,
                        now
                ));

        bindings.put(binding.bindingId(), binding);
        providerUserIndex.put(userKey(normalizedProvider, normalizedUserId), binding.bindingId());
        if (normalizedUnionId != null) {
            providerUnionIndex.put(unionKey(normalizedProvider, normalizedUnionId), binding.bindingId());
        }
        return binding;
    }

    private String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String userKey(String provider, String providerUserId) {
        return provider + "::USER::" + providerUserId;
    }

    private String unionKey(String provider, String providerUnionId) {
        return provider + "::UNION::" + providerUnionId;
    }

    private Map<String, String> copyMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(new LinkedHashMap<>(values));
    }
}
