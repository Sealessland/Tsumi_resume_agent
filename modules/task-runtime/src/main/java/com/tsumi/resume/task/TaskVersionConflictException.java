package com.tsumi.resume.task;

public final class TaskVersionConflictException extends RuntimeException {

    public TaskVersionConflictException(
            String taskId, long expectedRevision, long actualRevision) {
        super("Task revision conflict for %s: expected %d but was %d"
                .formatted(taskId, expectedRevision, actualRevision));
    }
}
