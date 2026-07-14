package com.tsumi.resume.task;

public final class TaskNotRetryableException extends RuntimeException {
    public TaskNotRetryableException(String taskId) {
        super("Task is not retryable: " + taskId);
    }
}
