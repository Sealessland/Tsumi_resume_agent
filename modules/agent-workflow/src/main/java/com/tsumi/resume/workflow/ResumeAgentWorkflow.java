package com.tsumi.resume.workflow;

public interface ResumeAgentWorkflow {

    WorkflowResult execute(WorkflowInput input);

    default WorkflowResult execute(WorkflowInput input, WorkflowObserver observer) {
        return execute(input);
    }
}
