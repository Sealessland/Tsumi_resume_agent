package com.tsumi.resume.workflow.review;

import com.tsumi.resume.task.TaskStatus;
import java.time.Instant;
import java.util.Map;

public record TaskTimelineItem(
        long eventId,
        String type,
        TaskStatus stage,
        int attempt,
        Instant occurredAt,
        Map<String, String> data) {

    public TaskTimelineItem {
        data = Map.copyOf(data);
    }
}
