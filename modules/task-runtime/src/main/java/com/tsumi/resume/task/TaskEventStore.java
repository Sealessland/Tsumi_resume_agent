package com.tsumi.resume.task;

import java.util.List;

public interface TaskEventStore {

    TaskEvent append(NewTaskEvent event);

    List<TaskEvent> findAfter(String taskId, long afterExclusive, int limit);
}
