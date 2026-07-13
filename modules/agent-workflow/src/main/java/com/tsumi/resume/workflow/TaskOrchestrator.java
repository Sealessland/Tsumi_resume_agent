package com.tsumi.resume.workflow;

import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskNotFoundException;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.workflow.resume.ResumeVersionReader;
import java.time.Clock;
import java.util.function.Supplier;

public final class TaskOrchestrator {

    private final TaskRepository taskRepository;
    private final ResumeVersionReader resumeVersionReader;
    private final ResumeAgentWorkflow workflow;
    private final Clock clock;
    private final Supplier<String> taskIdSupplier;

    public TaskOrchestrator(
            TaskRepository taskRepository,
            ResumeVersionReader resumeVersionReader,
            ResumeAgentWorkflow workflow,
            Clock clock,
            Supplier<String> taskIdSupplier) {
        this.taskRepository = taskRepository;
        this.resumeVersionReader = resumeVersionReader;
        this.workflow = workflow;
        this.clock = clock;
        this.taskIdSupplier = taskIdSupplier;
    }

    public ResumeTask create(CreateTaskCommand command) {
        resumeVersionReader.get(command.resumeId(), command.baseVersion());
        var taskId = taskIdSupplier.get();
        var created = ResumeTask.created(
                taskId, command.resumeId(), command.baseVersion(), clock.instant());
        taskRepository.save(created);

        var running = created.start(clock.instant());
        taskRepository.save(running);

        var workflowResult = workflow.execute(new WorkflowInput(
                taskId,
                command.resumeId(),
                command.baseVersion(),
                command.jobDescription()));
        var review = running.requireReview(workflowResult.summary(), clock.instant());
        return taskRepository.save(review);
    }

    public ResumeTask get(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }
}
