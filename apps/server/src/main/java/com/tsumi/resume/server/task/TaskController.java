package com.tsumi.resume.server.task;

import com.tsumi.resume.workflow.CreateTaskCommand;
import com.tsumi.resume.workflow.AsyncTaskService;
import com.tsumi.resume.server.api.HttpIdempotencyService;
import com.tsumi.resume.task.ResumeTask;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final AsyncTaskService tasks;
    private final TaskEventSseService taskEvents;
    private final HttpIdempotencyService idempotency;

    public TaskController(
            AsyncTaskService tasks,
            TaskEventSseService taskEvents,
            HttpIdempotencyService idempotency) {
        this.tasks = tasks;
        this.taskEvents = taskEvents;
        this.idempotency = idempotency;
    }

    @PostMapping
    ResponseEntity<TaskResponse> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateTaskRequest request) {
        var task = tasks.create(new CreateTaskCommand(
                request.resumeId(), request.baseVersion(), request.jobDescription()), idempotencyKey);
        var location = URI.create("/api/v1/tasks/" + task.taskId());
        return ResponseEntity.accepted().location(location).body(TaskResponse.from(task));
    }

    @GetMapping("/{taskId}")
    TaskResponse get(@PathVariable("taskId") String taskId) {
        return TaskResponse.from(tasks.get(taskId));
    }

    @PostMapping("/{taskId}/cancel")
    TaskResponse cancel(
            @PathVariable("taskId") String taskId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        var task = idempotency.execute(
                "task:cancel:" + taskId, idempotencyKey,
                java.util.Map.of("taskId", taskId), 200, ResumeTask.class,
                () -> tasks.cancel(taskId));
        return TaskResponse.from(task);
    }

    @PostMapping("/{taskId}/retry")
    TaskResponse retry(
            @PathVariable("taskId") String taskId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        var task = idempotency.execute(
                "task:retry:" + taskId, idempotencyKey,
                java.util.Map.of("taskId", taskId), 200, ResumeTask.class,
                () -> tasks.retry(taskId));
        return TaskResponse.from(task);
    }

    @GetMapping(value = "/{taskId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter events(
            @PathVariable("taskId") String taskId,
            @RequestHeader(value = "Last-Event-ID", defaultValue = "0") String lastEventId,
            @RequestHeader(value = "X-Session-ID", defaultValue = "browser") String sessionId) {
        try {
            return taskEvents.stream(taskId, Long.parseLong(lastEventId), sessionId);
        } catch (NumberFormatException exception) {
            throw new SseCursorInvalidException();
        }
    }
}
