package com.tsumi.resume.task;

public final class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String scope, String key) {
        super("Idempotency key was reused with a different request: " + scope + "/" + key);
    }
}
