package com.tsumi.resume.task;

public final class TaskCancelledException extends RuntimeException {
    public TaskCancelledException(String taskId) {
        super("Task has been cancelled: " + taskId);
    }
}
