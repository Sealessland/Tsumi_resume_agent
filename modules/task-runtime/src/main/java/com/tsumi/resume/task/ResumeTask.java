package com.tsumi.resume.task;

import java.time.Instant;
import java.util.Objects;

public record ResumeTask(
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
        String leaseOwner,
        Instant leaseUntil,
        long revision,
        Instant createdAt,
        Instant updatedAt) {

    public ResumeTask {
        requireText(taskId, "taskId");
        requireText(resumeId, "resumeId");
        requireText(traceId, "traceId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (baseVersion < 1 || attempt < 1 || repairCount < 0 || repairCount > 1
                || revision < 0) {
            throw new IllegalArgumentException("Invalid task execution counters");
        }
        if ((leaseOwner == null) != (leaseUntil == null)) {
            throw new IllegalArgumentException("Lease owner and expiry must be set together");
        }
    }

    public static ResumeTask created(
            String taskId,
            String resumeId,
            long baseVersion,
            String traceId,
            Instant now) {
        return new ResumeTask(
                taskId, resumeId, baseVersion, TaskStatus.CREATED,
                null, null, false, 1, 0, traceId,
                null, null, 0, now, now);
    }

    public ResumeTask analyze(Instant now) {
        requireStatus(TaskStatus.CREATED);
        return transition(TaskStatus.ANALYZING, workflowSummary, null, false,
                attempt, repairCount, null, null, now);
    }

    public ResumeTask propose(Instant now) {
        requireStatus(TaskStatus.ANALYZING);
        return transition(TaskStatus.PROPOSING, workflowSummary, null, false,
                attempt, repairCount, leaseOwner, leaseUntil, now);
    }

    public ResumeTask verify(Instant now) {
        requireStatus(TaskStatus.PROPOSING);
        return transition(TaskStatus.VERIFYING, workflowSummary, null, false,
                attempt, repairCount, leaseOwner, leaseUntil, now);
    }

    public ResumeTask repair(Instant now) {
        requireStatus(TaskStatus.VERIFYING);
        if (repairCount >= 1) {
            throw new IllegalStateException("Semantic repair limit has been reached");
        }
        return transition(TaskStatus.PROPOSING, workflowSummary, null, false,
                attempt, repairCount + 1, leaseOwner, leaseUntil, now);
    }

    public ResumeTask reviewReady(String summary, Instant now) {
        requireStatus(TaskStatus.VERIFYING);
        requireText(summary, "summary");
        return transition(TaskStatus.REVIEW_READY, summary, null, false,
                attempt, repairCount, null, null, now);
    }

    public ResumeTask approve(Instant now) {
        requireStatus(TaskStatus.REVIEW_READY);
        return transition(TaskStatus.APPROVED, workflowSummary, null, false,
                attempt, repairCount, null, null, now);
    }

    public ResumeTask complete(Instant now) {
        requireStatus(TaskStatus.APPROVED);
        return transition(TaskStatus.COMPLETED, workflowSummary, null, false,
                attempt, repairCount, null, null, now);
    }

    public ResumeTask fail(String code, boolean retryable, Instant now) {
        if (!status.isExecutionActive()) {
            throw new IllegalStateException("Cannot fail task in state " + status);
        }
        requireText(code, "failureCode");
        return transition(TaskStatus.FAILED, workflowSummary, code, retryable,
                attempt, repairCount, null, null, now);
    }

    public ResumeTask retry(Instant now) {
        requireStatus(TaskStatus.FAILED);
        if (!failureRetryable) {
            throw new IllegalStateException("Task failure is not retryable");
        }
        return transition(TaskStatus.ANALYZING, workflowSummary, null, false,
                attempt + 1, 0, null, null, now);
    }

    public ResumeTask cancel(Instant now) {
        if (!status.isExecutionActive() && status != TaskStatus.REVIEW_READY) {
            throw new IllegalStateException("Cannot cancel task in state " + status);
        }
        return transition(TaskStatus.CANCELLED, workflowSummary, null, false,
                attempt, repairCount, null, null, now);
    }

    public ResumeTask lease(String owner, Instant until, Instant now) {
        if (!status.isExecutionActive()) {
            throw new IllegalStateException("Cannot lease task in state " + status);
        }
        requireText(owner, "leaseOwner");
        Objects.requireNonNull(until, "leaseUntil");
        if (!until.isAfter(now)) {
            throw new IllegalArgumentException("Lease expiry must be after now");
        }
        return transition(status, workflowSummary, failureCode, failureRetryable,
                attempt, repairCount, owner, until, now);
    }

    public ResumeTask releaseLease(Instant now) {
        if (leaseOwner == null) {
            return this;
        }
        return transition(status, workflowSummary, failureCode, failureRetryable,
                attempt, repairCount, null, null, now);
    }

    private ResumeTask transition(
            TaskStatus nextStatus,
            String nextSummary,
            String nextFailureCode,
            boolean nextFailureRetryable,
            int nextAttempt,
            int nextRepairCount,
            String nextLeaseOwner,
            Instant nextLeaseUntil,
            Instant now) {
        Objects.requireNonNull(now, "now");
        return new ResumeTask(
                taskId, resumeId, baseVersion, nextStatus,
                nextSummary, nextFailureCode, nextFailureRetryable,
                nextAttempt, nextRepairCount, traceId,
                nextLeaseOwner, nextLeaseUntil, revision + 1,
                createdAt, now);
    }

    private void requireStatus(TaskStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Expected task state " + expected + " but was " + status);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
