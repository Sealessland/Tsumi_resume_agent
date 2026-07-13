package com.tsumi.resume.infrastructure.workflow;

import com.tsumi.resume.workflow.ResumeAgentWorkflow;
import com.tsumi.resume.workflow.WorkflowInput;
import com.tsumi.resume.workflow.WorkflowResult;

public final class LocalDeterministicWorkflow implements ResumeAgentWorkflow {

    public static final String SUMMARY = "LOCAL_FAKE_READY_FOR_REVIEW";

    @Override
    public WorkflowResult execute(WorkflowInput input) {
        return new WorkflowResult(SUMMARY);
    }
}
