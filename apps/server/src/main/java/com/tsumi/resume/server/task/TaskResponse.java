package com.tsumi.resume.server.task;

import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskStatus;
import java.time.Instant;

public record TaskResponse(
        String taskId,
        String resumeId,
        long baseVersion,
        TaskStatus status,
        String workflowSummary,
        String failureCode,
        boolean failureRetryable,
        int attempt,
        int repairCount,
        String traceId,
        long revision,
        Instant createdAt,
        Instant updatedAt) {

    static TaskResponse from(ResumeTask task) {
        return new TaskResponse(
                task.taskId(),
                task.resumeId(),
                task.baseVersion(),
                task.status(),
                task.workflowSummary(),
                task.failureCode(),
                task.failureRetryable(),
                task.attempt(),
                task.repairCount(),
                task.traceId(),
                task.revision(),
                task.createdAt(),
                task.updatedAt());
    }
}
