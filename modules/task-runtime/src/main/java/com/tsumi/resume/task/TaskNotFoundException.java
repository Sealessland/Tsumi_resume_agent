package com.tsumi.resume.task;

public final class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(String taskId) {
        super("Resume task not found: " + taskId);
    }
}
