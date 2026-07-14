package com.tsumi.resume.task;

import java.util.List;
import java.util.Optional;

public interface TaskEventStore {

    TaskEvent append(NewTaskEvent event);

    List<TaskEvent> findAfter(String taskId, long afterExclusive, int limit);

    default Optional<TaskEvent> findById(long eventId) {
        return Optional.empty();
    }
}
