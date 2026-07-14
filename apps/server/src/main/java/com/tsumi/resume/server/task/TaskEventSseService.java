package com.tsumi.resume.server.task;

import com.tsumi.resume.task.TaskEventStore;
import com.tsumi.resume.task.TaskNotFoundException;
import com.tsumi.resume.task.TaskRepository;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class TaskEventSseService {
    private final TaskEventStore events;
    private final TaskRepository tasks;
    private final Duration heartbeat;
    private final Duration pollInterval;
    private final Semaphore permits;
    private final ConcurrentHashMap<String, SseEmitter> active = new ConcurrentHashMap<>();

    public TaskEventSseService(
            TaskEventStore events,
            TaskRepository tasks,
            @Value("${tsumi.sse.max-connections:20}") int maxConnections,
            @Value("${tsumi.sse.heartbeat:15s}") Duration heartbeat,
            @Value("${tsumi.sse.poll-interval:100ms}") Duration pollInterval) {
        this.events = events;
        this.tasks = tasks;
        this.heartbeat = heartbeat;
        this.pollInterval = pollInterval;
        this.permits = new Semaphore(maxConnections);
    }

    public SseEmitter stream(String taskId, long cursor, String sessionId) {
        tasks.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        validateCursor(taskId, cursor);
        if (sessionId == null || sessionId.isBlank() || sessionId.length() > 120) {
            throw new IllegalArgumentException("X-Session-ID must be 1-120 characters");
        }
        if (!permits.tryAcquire()) {
            throw new SseConnectionRejectedException("SSE connection capacity has been reached", true);
        }
        var key = taskId + "\u0000" + sessionId;
        var emitter = new SseEmitter(0L);
        if (active.putIfAbsent(key, emitter) != null) {
            permits.release();
            throw new SseConnectionRejectedException("This session already has an event stream", false);
        }
        var released = new AtomicBoolean();
        Runnable release = () -> {
            if (released.compareAndSet(false, true)) {
                active.remove(key, emitter);
                permits.release();
            }
        };
        emitter.onCompletion(release);
        emitter.onTimeout(release);
        emitter.onError(error -> release.run());
        Thread.ofVirtual().name("task-sse-" + taskId).start(() -> pump(taskId, cursor, emitter, release));
        return emitter;
    }

    private void pump(String taskId, long initialCursor, SseEmitter emitter, Runnable release) {
        var cursor = initialCursor;
        var lastHeartbeat = Instant.now();
        try {
            while (true) {
                var batch = events.findAfter(taskId, cursor, 100);
                for (var event : batch) {
                    emitter.send(SseEmitter.event()
                            .id(Long.toString(event.eventId()))
                            .name(event.type())
                            .data(TaskEventEnvelope.from(event)));
                    cursor = event.eventId();
                }
                if (!batch.isEmpty()) continue;
                var task = tasks.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
                if (task.status().isTerminal()) {
                    emitter.complete();
                    return;
                }
                if (Duration.between(lastHeartbeat, Instant.now()).compareTo(heartbeat) >= 0) {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                    lastHeartbeat = Instant.now();
                }
                LockSupport.parkNanos(pollInterval.toNanos());
            }
        } catch (IOException | RuntimeException exception) {
            emitter.completeWithError(exception);
        } finally {
            release.run();
        }
    }

    private void validateCursor(String taskId, long cursor) {
        if (cursor < 0) throw new SseCursorInvalidException();
        if (cursor == 0) return;
        var event = events.findById(cursor).orElseThrow(SseCursorInvalidException::new);
        if (!event.taskId().equals(taskId)) throw new SseCursorInvalidException();
    }
}
