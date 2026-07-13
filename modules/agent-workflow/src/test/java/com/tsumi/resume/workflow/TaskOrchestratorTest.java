package com.tsumi.resume.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskNotFoundException;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.task.TaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TaskOrchestratorTest {

    @Test
    void persistsExplicitStatesAndSendsTypedWorkflowInput() {
        var repository = new RecordingTaskRepository();
        var workflow = new RecordingWorkflow();
        var clock = Clock.fixed(Instant.parse("2026-07-13T00:00:00Z"), ZoneOffset.UTC);
        var orchestrator = new TaskOrchestrator(repository, workflow, clock, () -> "task_01");

        var result = orchestrator.create(new CreateTaskCommand("res_01", 3, "Java Agent Engineer"));

        assertThat(repository.saved).extracting(ResumeTask::status)
                .containsExactly(TaskStatus.CREATED, TaskStatus.RUNNING, TaskStatus.REVIEW_REQUIRED);
        assertThat(workflow.received).isEqualTo(
                new WorkflowInput("task_01", "res_01", 3, "Java Agent Engineer"));
        assertThat(result.workflowSummary()).isEqualTo("LOCAL_FAKE_READY_FOR_REVIEW");
    }

    @Test
    void reportsUnknownTaskThroughTheDomainException() {
        var orchestrator = new TaskOrchestrator(
                new RecordingTaskRepository(), input -> new WorkflowResult("unused"),
                Clock.systemUTC(), () -> "task_01");

        assertThatThrownBy(() -> orchestrator.get("task_missing"))
                .isInstanceOf(TaskNotFoundException.class);
    }

    private static final class RecordingWorkflow implements ResumeAgentWorkflow {
        private WorkflowInput received;

        @Override
        public WorkflowResult execute(WorkflowInput input) {
            received = input;
            return new WorkflowResult("LOCAL_FAKE_READY_FOR_REVIEW");
        }
    }

    private static final class RecordingTaskRepository implements TaskRepository {
        private final List<ResumeTask> saved = new ArrayList<>();

        @Override
        public ResumeTask save(ResumeTask task) {
            saved.add(task);
            return task;
        }

        @Override
        public Optional<ResumeTask> findById(String taskId) {
            return saved.stream()
                    .filter(task -> task.taskId().equals(taskId))
                    .reduce((first, second) -> second);
        }
    }
}
