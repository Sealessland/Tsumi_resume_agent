package com.tsumi.resume.infrastructure.task;

import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskRepository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.time.Instant;
import java.util.List;

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

    @Override
    public List<ResumeTask> findRecoverable(Instant now, int limit) {
        return tasks.values().stream()
                .filter(task -> task.status().isExecutionActive())
                .filter(task -> task.leaseUntil() == null || task.leaseUntil().isBefore(now))
                .sorted(java.util.Comparator.comparing(ResumeTask::updatedAt))
                .limit(limit)
                .toList();
    }
}
