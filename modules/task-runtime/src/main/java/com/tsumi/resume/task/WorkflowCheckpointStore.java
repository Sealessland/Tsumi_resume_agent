package com.tsumi.resume.task;

import java.util.List;
import java.util.Optional;

public interface WorkflowCheckpointStore {

    WorkflowCheckpoint save(WorkflowCheckpoint checkpoint);

    Optional<WorkflowCheckpoint> findLatest(String taskId, WorkflowNode node);

    List<WorkflowCheckpoint> findByTaskId(String taskId);
}
