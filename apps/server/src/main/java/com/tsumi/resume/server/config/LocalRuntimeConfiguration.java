package com.tsumi.resume.server.config;

import com.tsumi.resume.infrastructure.task.InMemoryTaskRepository;
import com.tsumi.resume.infrastructure.workflow.LocalDeterministicWorkflow;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.workflow.ResumeAgentWorkflow;
import com.tsumi.resume.workflow.TaskOrchestrator;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalRuntimeConfiguration {

    @Bean
    TaskRepository taskRepository() {
        return new InMemoryTaskRepository();
    }

    @Bean
    ResumeAgentWorkflow resumeAgentWorkflow() {
        return new LocalDeterministicWorkflow();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    TaskOrchestrator taskOrchestrator(
            TaskRepository taskRepository,
            ResumeAgentWorkflow workflow,
            Clock clock) {
        return new TaskOrchestrator(
                taskRepository,
                workflow,
                clock,
                () -> "task_" + UUID.randomUUID().toString().replace("-", ""));
    }
}
