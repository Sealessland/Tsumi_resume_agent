package com.tsumi.resume.task;

import java.util.Optional;

public interface TaskRepository {

    ResumeTask save(ResumeTask task);

    Optional<ResumeTask> findById(String taskId);
}
