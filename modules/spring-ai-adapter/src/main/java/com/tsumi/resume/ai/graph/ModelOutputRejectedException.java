package com.tsumi.resume.ai.graph;

import com.tsumi.resume.workflow.WorkflowExecutionException;

public final class ModelOutputRejectedException extends WorkflowExecutionException {
    public ModelOutputRejectedException(String node, Throwable cause) {
        super("MODEL_OUTPUT_REJECTED", false,
                "Structured model output was rejected for " + node, cause);
    }

    public ModelOutputRejectedException(String node, String reason) {
        super("MODEL_OUTPUT_REJECTED", false,
                "Structured model output was rejected for " + node + ": " + reason);
    }
}
