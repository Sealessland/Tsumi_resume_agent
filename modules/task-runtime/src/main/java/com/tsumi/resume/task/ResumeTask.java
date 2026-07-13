package com.tsumi.resume.task;

import java.time.Instant;

public record ResumeTask(
        String taskId,
        String resumeId,
        long baseVersion,
        TaskStatus status,
        String workflowSummary,
        String failureCode,
        Instant createdAt,
        Instant updatedAt) {

    public static ResumeTask created(
            String taskId, String resumeId, long baseVersion, Instant now) {
        return new ResumeTask(
                taskId, resumeId, baseVersion, TaskStatus.CREATED, null, null, now, now);
    }

    public ResumeTask start(Instant now) {
        requireStatus(TaskStatus.CREATED);
        return new ResumeTask(
                taskId, resumeId, baseVersion, TaskStatus.RUNNING,
                workflowSummary, failureCode, createdAt, now);
    }

    public ResumeTask requireReview(String summary, Instant now) {
        requireStatus(TaskStatus.RUNNING);
        return new ResumeTask(
                taskId, resumeId, baseVersion, TaskStatus.REVIEW_REQUIRED,
                summary, null, createdAt, now);
    }

    public ResumeTask fail(String code, Instant now) {
        if (status != TaskStatus.CREATED && status != TaskStatus.RUNNING) {
            throw new IllegalStateException("Cannot fail task in state " + status);
        }
        return new ResumeTask(
                taskId, resumeId, baseVersion, TaskStatus.FAILED,
                workflowSummary, code, createdAt, now);
    }

    private void requireStatus(TaskStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Expected task state " + expected + " but was " + status);
        }
    }
}
