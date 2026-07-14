package com.tsumi.resume.infrastructure.task;

import com.tsumi.resume.task.NewTaskEvent;
import com.tsumi.resume.task.TaskEvent;
import com.tsumi.resume.task.TaskEventStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Optional;

public final class InMemoryTaskEventStore implements TaskEventStore {
    private final AtomicLong sequence = new AtomicLong();
    private final List<TaskEvent> events = new ArrayList<>();

    @Override
    public synchronized TaskEvent append(NewTaskEvent event) {
        var stored = new TaskEvent(
                sequence.incrementAndGet(), event.taskId(), event.type(), event.stage(),
                event.attempt(), event.traceId(), event.occurredAt(), event.data());
        events.add(stored);
        return stored;
    }

    @Override
    public synchronized List<TaskEvent> findAfter(String taskId, long afterExclusive, int limit) {
        if (afterExclusive < 0 || limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("Invalid event cursor or limit");
        }
        return events.stream()
                .filter(event -> event.taskId().equals(taskId) && event.eventId() > afterExclusive)
                .limit(limit)
                .toList();
    }

    @Override
    public synchronized Optional<TaskEvent> findById(long eventId) {
        return events.stream().filter(event -> event.eventId() == eventId).findFirst();
    }
}
