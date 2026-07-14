package com.tsumi.resume.server.task;

import com.tsumi.resume.workflow.TaskExecutionScheduler;
import com.tsumi.resume.workflow.TaskWorkflowRunner;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BoundedTaskExecutionScheduler implements TaskExecutionScheduler {
    private final TaskWorkflowRunner runner;
    private final ThreadPoolExecutor executor;
    private final Set<String> scheduled = ConcurrentHashMap.newKeySet();

    public BoundedTaskExecutionScheduler(TaskWorkflowRunner runner, int concurrency, int queueCapacity) {
        this.runner = runner;
        var counter = new AtomicInteger();
        ThreadFactory threads = runnable -> {
            var thread = new Thread(runnable, "resume-workflow-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        executor = new ThreadPoolExecutor(
                concurrency, concurrency, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), threads,
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    public void schedule(String taskId) {
        if (!scheduled.add(taskId)) return;
        try {
            executor.execute(() -> {
                try {
                    runner.run(taskId);
                } finally {
                    scheduled.remove(taskId);
                }
            });
        } catch (RuntimeException exception) {
            scheduled.remove(taskId);
            throw exception;
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
