package com.tsumi.resume.infrastructure.workflow;

import com.tsumi.resume.workflow.WorkflowRequest;
import com.tsumi.resume.workflow.WorkflowRequestStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryWorkflowRequestStore implements WorkflowRequestStore {
    private final Map<String, WorkflowRequest> requests = new LinkedHashMap<>();

    @Override
    public synchronized WorkflowRequest save(WorkflowRequest request) {
        var existing = requests.get(request.taskId());
        if (existing != null && !existing.equals(request)) {
            throw new IllegalStateException("Workflow request is immutable: " + request.taskId());
        }
        requests.putIfAbsent(request.taskId(), request);
        return request;
    }

    @Override
    public synchronized Optional<WorkflowRequest> find(String taskId) {
        return Optional.ofNullable(requests.get(taskId));
    }
}
