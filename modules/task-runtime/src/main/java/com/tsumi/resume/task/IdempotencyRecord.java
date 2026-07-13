package com.tsumi.resume.task;

import java.time.Instant;
import java.util.Objects;

public record IdempotencyRecord(
        String scope,
        String key,
        String requestHash,
        int responseStatus,
        String responseBody,
        Instant createdAt) {
    public IdempotencyRecord {
        requireText(scope, "scope"); requireText(key, "key"); requireText(requestHash, "requestHash");
        requireText(responseBody, "responseBody"); Objects.requireNonNull(createdAt, "createdAt");
        if (responseStatus < 100 || responseStatus > 599) throw new IllegalArgumentException("Invalid HTTP status");
    }
    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
