package com.tsumi.resume.workflow;

import java.time.Instant;
import java.util.Objects;

public record WorkflowRequest(String taskId, String jobDescription, Instant createdAt) {
    public WorkflowRequest {
        if (taskId == null || taskId.isBlank()) throw new IllegalArgumentException("taskId must not be blank");
        if (jobDescription == null || jobDescription.isBlank()) {
            throw new IllegalArgumentException("jobDescription must not be blank");
        }
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
