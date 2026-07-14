package com.tsumi.resume.task;

import java.util.Optional;
import java.util.List;
import java.time.Instant;

public interface TaskRepository {

    ResumeTask save(ResumeTask task);

    Optional<ResumeTask> findById(String taskId);

    default List<ResumeTask> findRecoverable(Instant now, int limit) {
        return List.of();
    }
}
