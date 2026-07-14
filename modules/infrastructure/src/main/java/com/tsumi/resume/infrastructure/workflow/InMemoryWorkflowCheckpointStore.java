package com.tsumi.resume.infrastructure.workflow;

import com.tsumi.resume.task.WorkflowCheckpoint;
import com.tsumi.resume.task.WorkflowCheckpointStore;
import com.tsumi.resume.task.WorkflowNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InMemoryWorkflowCheckpointStore implements WorkflowCheckpointStore {
    private final List<WorkflowCheckpoint> checkpoints = new ArrayList<>();

    @Override
    public synchronized WorkflowCheckpoint save(WorkflowCheckpoint checkpoint) {
        checkpoints.add(checkpoint);
        return checkpoint;
    }

    @Override
    public synchronized Optional<WorkflowCheckpoint> findLatest(String taskId, WorkflowNode node) {
        return checkpoints.stream()
                .filter(checkpoint -> checkpoint.taskId().equals(taskId) && checkpoint.node() == node)
                .reduce((first, second) -> second);
    }

    @Override
    public synchronized List<WorkflowCheckpoint> findByTaskId(String taskId) {
        return checkpoints.stream().filter(checkpoint -> checkpoint.taskId().equals(taskId)).toList();
    }
}
