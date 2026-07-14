package com.tsumi.resume.ai.graph;

public final class WorkflowModelTimeoutException extends RuntimeException {
    public WorkflowModelTimeoutException(String node, Throwable cause) {
        super("Model call timed out for " + node, cause);
    }
}
