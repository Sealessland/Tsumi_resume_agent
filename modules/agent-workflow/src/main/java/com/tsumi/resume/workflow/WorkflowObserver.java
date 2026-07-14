package com.tsumi.resume.workflow;

import com.tsumi.resume.task.WorkflowNode;

public interface WorkflowObserver {
    void nodeStarted(WorkflowNode node);
    void nodeCompleted(WorkflowNode node, long durationMillis);
    void nodeFailed(WorkflowNode node, String errorCode, long durationMillis);

    static WorkflowObserver noop() {
        return new WorkflowObserver() {
            @Override public void nodeStarted(WorkflowNode node) {}
            @Override public void nodeCompleted(WorkflowNode node, long durationMillis) {}
            @Override public void nodeFailed(WorkflowNode node, String errorCode, long durationMillis) {}
        };
    }
}
