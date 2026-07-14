package com.tsumi.resume.persistence.workflow;

import com.tsumi.resume.workflow.WorkflowRequest;
import com.tsumi.resume.workflow.WorkflowRequestStore;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaWorkflowRequestStore implements WorkflowRequestStore {
    private final WorkflowRequestJpaRepository repository;

    public JpaWorkflowRequestStore(WorkflowRequestJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public WorkflowRequest save(WorkflowRequest request) {
        var existing = repository.findById(request.taskId()).map(WorkflowRequestEntity::toDomain);
        if (existing.isPresent()) {
            if (existing.orElseThrow().equals(request)) return request;
            throw new IllegalStateException("Workflow request is immutable: " + request.taskId());
        }
        repository.saveAndFlush(new WorkflowRequestEntity(request));
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkflowRequest> find(String taskId) {
        return repository.findById(taskId).map(WorkflowRequestEntity::toDomain);
    }
}
