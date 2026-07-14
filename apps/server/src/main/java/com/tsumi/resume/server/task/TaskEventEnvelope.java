package com.tsumi.resume.server.task;

import com.tsumi.resume.task.TaskEvent;
import com.tsumi.resume.task.TaskStatus;
import java.time.Instant;
import java.util.Map;

public record TaskEventEnvelope(
        long eventId,
        String taskId,
        String type,
        TaskStatus stage,
        int attempt,
        String traceId,
        Instant occurredAt,
        Map<String, String> data) {

    static TaskEventEnvelope from(TaskEvent event) {
        return new TaskEventEnvelope(
                event.eventId(), event.taskId(), event.type(), event.stage(), event.attempt(),
                event.traceId(), event.occurredAt(), event.data());
    }
}
