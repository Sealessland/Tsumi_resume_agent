package com.tsumi.resume.server.config;

import com.tsumi.resume.task.IdempotencyStore;
import com.tsumi.resume.task.TaskEventStore;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.task.WorkflowCheckpointStore;
import com.tsumi.resume.server.task.BoundedTaskExecutionScheduler;
import com.tsumi.resume.workflow.AsyncTaskService;
import com.tsumi.resume.workflow.ResumeAgentWorkflow;
import com.tsumi.resume.workflow.TaskExecutionScheduler;
import com.tsumi.resume.workflow.TaskWorkflowRunner;
import com.tsumi.resume.workflow.UnitOfWork;
import com.tsumi.resume.workflow.WorkflowRequestStore;
import com.tsumi.resume.workflow.resume.ResumeVersionReader;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class AsyncTaskRuntimeConfiguration {

    @Bean
    TaskWorkflowRunner taskWorkflowRunner(
            TaskRepository tasks,
            WorkflowRequestStore requests,
            ResumeAgentWorkflow workflow,
            TaskEventStore events,
            UnitOfWork unitOfWork,
            WorkflowCheckpointStore checkpoints,
            Clock clock) {
        return new TaskWorkflowRunner(
                tasks, requests, workflow, events, unitOfWork, checkpoints, clock,
                "worker_" + UUID.randomUUID().toString().replace("-", ""), Duration.ofMinutes(1));
    }

    @Bean
    BoundedTaskExecutionScheduler taskExecutionScheduler(TaskWorkflowRunner runner) {
        return new BoundedTaskExecutionScheduler(runner, 2, 20);
    }

    @Bean
    AsyncTaskService asyncTaskService(
            TaskRepository tasks,
            ResumeVersionReader resumes,
            WorkflowRequestStore requests,
            TaskEventStore events,
            IdempotencyStore idempotency,
            UnitOfWork unitOfWork,
            TaskExecutionScheduler scheduler,
            Clock clock) {
        return new AsyncTaskService(
                tasks, resumes, requests, events, idempotency, unitOfWork,
                scheduler, clock,
                () -> "task_" + UUID.randomUUID().toString().replace("-", ""));
    }
}
