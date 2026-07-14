package com.tsumi.resume.workflow;

import com.tsumi.resume.task.IdempotencyConflictException;
import com.tsumi.resume.task.IdempotencyRecord;
import com.tsumi.resume.task.IdempotencyStore;
import com.tsumi.resume.task.NewTaskEvent;
import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskEventStore;
import com.tsumi.resume.task.TaskNotFoundException;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.workflow.resume.ResumeVersionReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Supplier;

public final class AsyncTaskService {

    private static final String CREATE_SCOPE = "task:create";

    private final TaskRepository tasks;
    private final ResumeVersionReader resumes;
    private final WorkflowRequestStore requests;
    private final TaskEventStore events;
    private final IdempotencyStore idempotency;
    private final UnitOfWork unitOfWork;
    private final TaskExecutionScheduler scheduler;
    private final Clock clock;
    private final Supplier<String> taskIdSupplier;

    public AsyncTaskService(
            TaskRepository tasks,
            ResumeVersionReader resumes,
            WorkflowRequestStore requests,
            TaskEventStore events,
            IdempotencyStore idempotency,
            UnitOfWork unitOfWork,
            TaskExecutionScheduler scheduler,
            Clock clock,
            Supplier<String> taskIdSupplier) {
        this.tasks = tasks;
        this.resumes = resumes;
        this.requests = requests;
        this.events = events;
        this.idempotency = idempotency;
        this.unitOfWork = unitOfWork;
        this.scheduler = scheduler;
        this.clock = clock;
        this.taskIdSupplier = taskIdSupplier;
    }

    public ResumeTask create(CreateTaskCommand command, String idempotencyKey) {
        requireIdempotencyKey(idempotencyKey);
        var requestHash = sha256(command.resumeId() + "\u0000" + command.baseVersion()
                + "\u0000" + command.jobDescription());
        var result = unitOfWork.execute(() -> createAtomically(command, idempotencyKey, requestHash));
        if (!result.replayed()) scheduler.schedule(result.task().taskId());
        return result.task();
    }

    public ResumeTask get(String taskId) {
        return tasks.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    public ResumeTask cancel(String taskId) {
        return unitOfWork.execute(() -> {
            var current = get(taskId);
            var cancelled = tasks.save(current.cancel(clock.instant()));
            append(cancelled, "task.cancelled", Map.of("cancelledBy", "human"));
            return cancelled;
        });
    }

    public ResumeTask retry(String taskId) {
        var retried = unitOfWork.execute(() -> {
            var current = get(taskId);
            requests.find(taskId).orElseThrow(() -> new IllegalStateException("Workflow request is missing"));
            var next = tasks.save(current.retry(clock.instant()));
            append(next, "task.retry.requested", Map.of("attempt", Integer.toString(next.attempt())));
            return next;
        });
        scheduler.schedule(taskId);
        return retried;
    }

    private Creation createAtomically(
            CreateTaskCommand command,
            String key,
            String requestHash) {
        var existing = idempotency.find(CREATE_SCOPE, key);
        if (existing.isPresent()) {
            var record = existing.orElseThrow();
            if (!record.requestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(CREATE_SCOPE, key);
            }
            return new Creation(get(record.responseBody()), true);
        }

        resumes.get(command.resumeId(), command.baseVersion());
        var now = clock.instant();
        var taskId = taskIdSupplier.get();
        var created = ResumeTask.created(
                taskId,
                command.resumeId(),
                command.baseVersion(),
                "trace_" + taskId.substring("task_".length()),
                now);
        var saved = tasks.save(created);
        requests.save(new WorkflowRequest(taskId, command.jobDescription(), now));
        append(saved, "task.created", Map.of("eventsUrl", "/api/v1/tasks/" + taskId + "/events"));
        idempotency.save(new IdempotencyRecord(
                CREATE_SCOPE, key, requestHash, 202, taskId, now));
        return new Creation(saved, false);
    }

    private void append(ResumeTask task, String type, Map<String, String> data) {
        events.append(new NewTaskEvent(
                task.taskId(), type, task.status(), task.attempt(), task.traceId(), clock.instant(), data));
    }

    private void requireIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 200) {
            throw new IllegalArgumentException("Idempotency-Key is required and must not exceed 200 characters");
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record Creation(ResumeTask task, boolean replayed) {}
}
