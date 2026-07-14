package com.tsumi.resume.ai.graph;

import com.tsumi.resume.workflow.WorkflowExecutionException;

public final class WorkflowModelTimeoutException extends WorkflowExecutionException {
    public WorkflowModelTimeoutException(String node, Throwable cause) {
        super("WORKFLOW_TIMEOUT", true, "Model call timed out for " + node, cause);
    }
}
