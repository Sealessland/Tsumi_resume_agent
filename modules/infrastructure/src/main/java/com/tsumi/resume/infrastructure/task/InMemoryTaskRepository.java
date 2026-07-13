package com.tsumi.resume.infrastructure.task;

import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskRepository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryTaskRepository implements TaskRepository {

    private final ConcurrentMap<String, ResumeTask> tasks = new ConcurrentHashMap<>();

    @Override
    public ResumeTask save(ResumeTask task) {
        tasks.put(task.taskId(), task);
        return task;
    }

    @Override
    public Optional<ResumeTask> findById(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }
}
