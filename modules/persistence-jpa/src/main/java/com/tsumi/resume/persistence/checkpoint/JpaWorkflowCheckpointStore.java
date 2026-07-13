package com.tsumi.resume.persistence.checkpoint;

import com.tsumi.resume.task.CheckpointStatus;
import com.tsumi.resume.task.WorkflowCheckpoint;
import com.tsumi.resume.task.WorkflowCheckpointStore;
import com.tsumi.resume.task.WorkflowNode;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaWorkflowCheckpointStore implements WorkflowCheckpointStore {
    private final WorkflowCheckpointJpaRepository repository;
    public JpaWorkflowCheckpointStore(WorkflowCheckpointJpaRepository repository) { this.repository = repository; }
    @Override @Transactional
    public WorkflowCheckpoint save(WorkflowCheckpoint checkpoint) {
        repository.saveAndFlush(new WorkflowCheckpointEntity(checkpoint));
        return checkpoint;
    }
    @Override @Transactional(readOnly = true)
    public Optional<WorkflowCheckpoint> findLatest(String taskId, WorkflowNode node) {
        return repository.findFirstByTaskIdAndNodeNameOrderByIdDesc(taskId, node.name()).map(this::toDomain);
    }
    @Override @Transactional(readOnly = true)
    public List<WorkflowCheckpoint> findByTaskId(String taskId) {
        return repository.findByTaskIdOrderByIdAsc(taskId).stream().map(this::toDomain).toList();
    }
    private WorkflowCheckpoint toDomain(WorkflowCheckpointEntity entity) {
        return new WorkflowCheckpoint(entity.taskId(), WorkflowNode.valueOf(entity.nodeName()),
                entity.attempt(), CheckpointStatus.valueOf(entity.status()), entity.inputHash(),
                entity.outputHash(), entity.errorCode(), entity.updatedAt());
    }
}
