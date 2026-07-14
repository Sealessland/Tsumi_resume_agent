package com.tsumi.resume.workflow;

import java.util.Optional;

public interface WorkflowRequestStore {
    WorkflowRequest save(WorkflowRequest request);
    Optional<WorkflowRequest> find(String taskId);
}
