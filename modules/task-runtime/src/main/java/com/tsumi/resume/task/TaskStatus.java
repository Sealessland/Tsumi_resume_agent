package com.tsumi.resume.task;

public enum TaskStatus {
    CREATED,
    ANALYZING,
    PROPOSING,
    VERIFYING,
    REVIEW_READY,
    APPROVED,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isExecutionActive() {
        return this == CREATED
                || this == ANALYZING
                || this == PROPOSING
                || this == VERIFYING;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
