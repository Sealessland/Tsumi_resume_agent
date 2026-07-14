package com.tsumi.resume.server.task;

import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.workflow.TaskExecutionScheduler;
import java.time.Clock;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskRecoveryCoordinator {
    private final TaskRepository tasks;
    private final TaskExecutionScheduler scheduler;
    private final Clock clock;

    public TaskRecoveryCoordinator(
            TaskRepository tasks,
            TaskExecutionScheduler scheduler,
            Clock clock) {
        this.tasks = tasks;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void afterStartup() {
        recover();
    }

    @Scheduled(fixedDelayString = "${tsumi.workflow.recovery-interval:30s}")
    public void recover() {
        for (var task : tasks.findRecoverable(clock.instant(), 20)) {
            try {
                scheduler.schedule(task.taskId());
            } catch (RejectedExecutionException saturated) {
                return;
            }
        }
    }
}
