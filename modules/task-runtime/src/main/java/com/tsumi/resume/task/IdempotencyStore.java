package com.tsumi.resume.task;

import java.util.Optional;

public interface IdempotencyStore {
    IdempotencyRecord save(IdempotencyRecord record);
    Optional<IdempotencyRecord> find(String scope, String key);
}
