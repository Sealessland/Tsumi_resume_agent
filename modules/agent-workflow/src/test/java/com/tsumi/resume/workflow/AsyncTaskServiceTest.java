package com.tsumi.resume.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.tsumi.resume.task.IdempotencyConflictException;
import com.tsumi.resume.task.IdempotencyRecord;
import com.tsumi.resume.task.IdempotencyStore;
import com.tsumi.resume.task.NewTaskEvent;
import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskEvent;
import com.tsumi.resume.task.TaskEventStore;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.task.TaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AsyncTaskServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");

    @Test
    void createsDurableTaskAndReturnsBeforeWorkflowExecution() {
        var fixture = new Fixture();

        var task = fixture.service.create(
                new CreateTaskCommand("res_01", 1, "Java Agent Engineer"), "idem_01");

        assertThat(task.status()).isEqualTo(TaskStatus.CREATED);
        assertThat(fixture.requests.find("task_01")).get()
                .extracting(WorkflowRequest::jobDescription).isEqualTo("Java Agent Engineer");
        assertThat(fixture.scheduled).containsExactly("task_01");
        assertThat(fixture.events.events).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo("task.created");
            assertThat(event.stage()).isEqualTo(TaskStatus.CREATED);
        });
    }

    @Test
    void replaysSameIdempotentCreateAndRejectsPayloadReuse() {
        var fixture = new Fixture();
        var command = new CreateTaskCommand("res_01", 1, "Java Agent Engineer");

        var first = fixture.service.create(command, "idem_01");
        var replay = fixture.service.create(command, "idem_01");

        assertThat(replay.taskId()).isEqualTo(first.taskId());
        assertThat(fixture.scheduled).containsExactly("task_01");
        assertThatThrownBy(() -> fixture.service.create(
                new CreateTaskCommand("res_01", 1, "不同 JD"), "idem_01"))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void cancelAndRetryArePersistedAndScheduledAccordingToStateRules() {
        var fixture = new Fixture();
        var task = fixture.service.create(
                new CreateTaskCommand("res_01", 1, "Java"), "idem_01");

        var cancelled = fixture.service.cancel(task.taskId());
        assertThat(cancelled.status()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(fixture.events.events.getLast().type()).isEqualTo("task.cancelled");

        var failed = ResumeTask.created("task_failed", "res_01", 1, "trace_failed", NOW)
                .analyze(NOW).fail("WORKFLOW_TIMEOUT", true, NOW);
        fixture.tasks.save(failed);
        fixture.requests.save(new WorkflowRequest("task_failed", "Java", NOW));

        var retried = fixture.service.retry("task_failed");
        assertThat(retried.status()).isEqualTo(TaskStatus.ANALYZING);
        assertThat(retried.attempt()).isEqualTo(2);
        assertThat(fixture.scheduled).containsExactly("task_01", "task_failed");
        assertThat(fixture.events.events.getLast().type()).isEqualTo("task.retry.requested");
    }

    private static final class Fixture {
        final MemoryTasks tasks = new MemoryTasks();
        final MemoryRequests requests = new MemoryRequests();
        final MemoryEvents events = new MemoryEvents();
        final MemoryIdempotency idempotency = new MemoryIdempotency();
        final List<String> scheduled = new ArrayList<>();
        final AsyncTaskService service;

        Fixture() {
            service = new AsyncTaskService(
                    tasks,
                    (resumeId, version) -> JsonNodeFactory.instance.objectNode()
                            .put("schemaVersion", 13).put("resumeId", resumeId).put("version", version),
                    requests,
                    events,
                    idempotency,
                    UnitOfWork.direct(),
                    scheduled::add,
                    Clock.fixed(NOW, ZoneOffset.UTC),
                    () -> "task_01");
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

    private static final class MemoryIdempotency implements IdempotencyStore {
        final Map<String, IdempotencyRecord> data = new LinkedHashMap<>();
        @Override public IdempotencyRecord save(IdempotencyRecord record) {
            data.put(record.scope() + ":" + record.key(), record); return record;
        }
        @Override public Optional<IdempotencyRecord> find(String scope, String key) {
            return Optional.ofNullable(data.get(scope + ":" + key));
        }
    }
}
