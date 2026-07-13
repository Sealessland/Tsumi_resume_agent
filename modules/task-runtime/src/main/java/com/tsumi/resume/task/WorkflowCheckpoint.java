package com.tsumi.resume.task;

import java.time.Instant;
import java.util.Objects;

public record WorkflowCheckpoint(
        String taskId,
        WorkflowNode node,
        int attempt,
        CheckpointStatus status,
        String inputHash,
        String outputHash,
        String errorCode,
        Instant updatedAt) {

    public WorkflowCheckpoint {
        requireText(taskId, "taskId");
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be positive");
        }
        if (status == CheckpointStatus.COMPLETED) {
            requireText(inputHash, "inputHash");
            requireText(outputHash, "outputHash");
        }
        if (status == CheckpointStatus.FAILED) {
            requireText(errorCode, "errorCode");
        }
    }

    public static WorkflowCheckpoint started(
            String taskId,
            WorkflowNode node,
            int attempt,
            String inputHash,
            Instant now) {
        return new WorkflowCheckpoint(
                taskId, node, attempt, CheckpointStatus.STARTED,
                inputHash, null, null, now);
    }

    public static WorkflowCheckpoint completed(
            String taskId,
            WorkflowNode node,
            int attempt,
            String inputHash,
            String outputHash,
            Instant now) {
        return new WorkflowCheckpoint(
                taskId, node, attempt, CheckpointStatus.COMPLETED,
                inputHash, outputHash, null, now);
    }

    public static WorkflowCheckpoint failed(
            String taskId,
            WorkflowNode node,
            int attempt,
            String inputHash,
            String errorCode,
            Instant now) {
        return new WorkflowCheckpoint(
                taskId, node, attempt, CheckpointStatus.FAILED,
                inputHash, null, errorCode, now);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
