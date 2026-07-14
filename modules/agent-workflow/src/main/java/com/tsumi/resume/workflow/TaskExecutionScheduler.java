package com.tsumi.resume.workflow;

@FunctionalInterface
public interface TaskExecutionScheduler {
    void schedule(String taskId);
}
