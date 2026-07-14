package com.tsumi.resume.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.tsumi.resume.task.NewTaskEvent;
import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskEvent;
import com.tsumi.resume.task.TaskEventStore;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.task.TaskStatus;
import com.tsumi.resume.task.WorkflowCheckpoint;
import com.tsumi.resume.task.WorkflowCheckpointStore;
import com.tsumi.resume.task.WorkflowNode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TaskWorkflowRunnerTest {

    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");

    @Test
    void executesCreatedTaskThroughReviewReadyAndEmitsOrderedStateEvents() {
        var fixture = new Fixture(input -> new WorkflowResult("READY_WITH_ONE_PATCH"));
        fixture.create("task_01");

        fixture.runner.run("task_01");

        var task = fixture.tasks.findById("task_01").orElseThrow();
        assertThat(task.status()).isEqualTo(TaskStatus.REVIEW_READY);
        assertThat(task.workflowSummary()).isEqualTo("READY_WITH_ONE_PATCH");
        assertThat(task.leaseOwner()).isNull();
        assertThat(fixture.events.events).extracting(TaskEvent::type).containsSubsequence(
                "task.lease.acquired", "task.state.changed", "task.state.changed",
                "task.state.changed", "task.review.ready");
    }

    @Test
    void cancelledTaskNeverInvokesWorkflow() {
        var calls = new AtomicInteger();
        var fixture = new Fixture(input -> {
            calls.incrementAndGet();
            return new WorkflowResult("unreachable");
        });
        fixture.create("task_01");
        var created = fixture.tasks.findById("task_01").orElseThrow();
        fixture.tasks.save(created.cancel(NOW));

        fixture.runner.run("task_01");

        assertThat(calls).hasValue(0);
        assertThat(fixture.tasks.findById("task_01").orElseThrow().status())
                .isEqualTo(TaskStatus.CANCELLED);
    }

    @Test
    void timeoutBecomesRetryableFailedTaskWithoutLeakingModelContent() {
        var fixture = new Fixture(input -> {
            throw new WorkflowExecutionException("WORKFLOW_TIMEOUT", true, "provider body must not escape");
        });
        fixture.create("task_01");

        fixture.runner.run("task_01");

        var task = fixture.tasks.findById("task_01").orElseThrow();
        assertThat(task.status()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.failureCode()).isEqualTo("WORKFLOW_TIMEOUT");
        assertThat(task.failureRetryable()).isTrue();
        assertThat(fixture.events.events.getLast().data().toString())
                .doesNotContain("provider body must not escape");
    }

    @Test
    void persistsNodeCheckpointsForRestartAudit() {
        ResumeAgentWorkflow observable = new ResumeAgentWorkflow() {
            @Override public WorkflowResult execute(WorkflowInput input) { return new WorkflowResult("READY"); }
            @Override public WorkflowResult execute(WorkflowInput input, WorkflowObserver observer) {
                observer.nodeStarted(WorkflowNode.JD_ANALYST);
                observer.nodeCompleted(WorkflowNode.JD_ANALYST, 12);
                return new WorkflowResult("READY");
            }
        };
        var fixture = new Fixture(observable);
        fixture.create("task_01");

        fixture.runner.run("task_01");

        assertThat(fixture.checkpoints.values)
                .extracting(WorkflowCheckpoint::status)
                .containsExactly(
                        com.tsumi.resume.task.CheckpointStatus.STARTED,
                        com.tsumi.resume.task.CheckpointStatus.COMPLETED);
    }

    private static final class Fixture {
        final MemoryTasks tasks = new MemoryTasks();
        final MemoryRequests requests = new MemoryRequests();
        final MemoryEvents events = new MemoryEvents();
        final MemoryCheckpoints checkpoints = new MemoryCheckpoints();
        final TaskWorkflowRunner runner;

        Fixture(ResumeAgentWorkflow workflow) {
            runner = new TaskWorkflowRunner(
                    tasks, requests, workflow, events, UnitOfWork.direct(),
                    checkpoints, Clock.fixed(NOW, ZoneOffset.UTC),
                    "worker_test", Duration.ofMinutes(1));
        }

        void create(String taskId) {
            tasks.save(ResumeTask.created(taskId, "res_01", 1, "trace_01", NOW));
            requests.save(new WorkflowRequest(taskId, "Java Agent Engineer", NOW));
        }
    }

    private static final class MemoryCheckpoints implements WorkflowCheckpointStore {
        final List<WorkflowCheckpoint> values = new ArrayList<>();
        @Override public WorkflowCheckpoint save(WorkflowCheckpoint checkpoint) { values.add(checkpoint); return checkpoint; }
        @Override public Optional<WorkflowCheckpoint> findLatest(String taskId, WorkflowNode node) {
            return values.stream().filter(value -> value.taskId().equals(taskId) && value.node() == node)
                    .reduce((first, second) -> second);
        }
        @Override public List<WorkflowCheckpoint> findByTaskId(String taskId) {
            return values.stream().filter(value -> value.taskId().equals(taskId)).toList();
        }
    }

    private static final class MemoryTasks implements TaskRepository {
        final Map<String, ResumeTask> data = new LinkedHashMap<>();
        @Override public ResumeTask save(ResumeTask task) { data.put(task.taskId(), task); return task; }
        @Override public Optional<ResumeTask> findById(String taskId) { return Optional.ofNullable(data.get(taskId)); }
    }

    private static final class MemoryRequests implements WorkflowRequestStore {
        final Map<String, WorkflowRequest> data = new LinkedHashMap<>();
        @Override public WorkflowRequest save(WorkflowRequest request) { data.put(request.taskId(), request); return request; }
        @Override public Optional<WorkflowRequest> find(String taskId) { return Optional.ofNullable(data.get(taskId)); }
    }

    private static final class MemoryEvents implements TaskEventStore {
        final List<TaskEvent> events = new ArrayList<>();
        @Override public TaskEvent append(NewTaskEvent event) {
            var stored = new TaskEvent(events.size() + 1L, event.taskId(), event.type(), event.stage(),
                    event.attempt(), event.traceId(), event.occurredAt(), event.data());
            events.add(stored); return stored;
        }
        @Override public List<TaskEvent> findAfter(String taskId, long afterExclusive, int limit) {
            return events.stream().filter(e -> e.taskId().equals(taskId) && e.eventId() > afterExclusive)
                    .limit(limit).toList();
        }
    }
}
