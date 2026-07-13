package com.tsumi.resume.task;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record NewTaskEvent(
        String taskId,
        String type,
        TaskStatus stage,
        int attempt,
        String traceId,
        Instant occurredAt,
        Map<String, String> data) {

    public NewTaskEvent {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be positive");
        }
        requireText(taskId, "taskId");
        requireText(type, "type");
        requireText(traceId, "traceId");
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(occurredAt, "occurredAt");
        data = Map.copyOf(data);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
