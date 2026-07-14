package com.tsumi.resume.infrastructure.task;

import com.tsumi.resume.task.IdempotencyConflictException;
import com.tsumi.resume.task.IdempotencyRecord;
import com.tsumi.resume.task.IdempotencyStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryIdempotencyStore implements IdempotencyStore {
    private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

    @Override
    public synchronized IdempotencyRecord save(IdempotencyRecord record) {
        var storageKey = record.scope() + "\u0000" + record.key();
        var existing = records.get(storageKey);
        if (existing != null && !existing.requestHash().equals(record.requestHash())) {
            throw new IdempotencyConflictException(record.scope(), record.key());
        }
        if (existing != null) return existing;
        records.put(storageKey, record);
        return record;
    }

    @Override
    public synchronized Optional<IdempotencyRecord> find(String scope, String key) {
        return Optional.ofNullable(records.get(scope + "\u0000" + key));
    }
}
