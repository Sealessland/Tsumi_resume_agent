package com.tsumi.resume.server.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsumi.resume.task.IdempotencyConflictException;
import com.tsumi.resume.task.IdempotencyRecord;
import com.tsumi.resume.task.IdempotencyStore;
import com.tsumi.resume.workflow.UnitOfWork;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class HttpIdempotencyService {
    private final IdempotencyStore records;
    private final UnitOfWork unitOfWork;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public HttpIdempotencyService(
            IdempotencyStore records,
            UnitOfWork unitOfWork,
            ObjectMapper objectMapper,
            Clock clock) {
        this.records = records;
        this.unitOfWork = unitOfWork;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public <T> T execute(
            String scope,
            String key,
            Object request,
            int responseStatus,
            Class<T> responseType,
            Supplier<T> operation) {
        requireKey(key);
        var requestHash = hash(request);
        return unitOfWork.execute(() -> {
            var existing = records.find(scope, key);
            if (existing.isPresent()) {
                var record = existing.orElseThrow();
                if (!record.requestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException(scope, key);
                }
                return read(record.responseBody(), responseType);
            }
            var response = operation.get();
            records.save(new IdempotencyRecord(
                    scope, key, requestHash, responseStatus, write(response), clock.instant()));
            return response;
        });
    }

    private String hash(Object request) {
        return sha256(write(request));
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Idempotent request cannot be serialized", exception);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored idempotent response is invalid", exception);
        }
    }

    private void requireKey(String key) {
        if (key == null || key.isBlank() || key.length() > 200) {
            throw new IllegalArgumentException("Idempotency-Key is required and must not exceed 200 characters");
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
