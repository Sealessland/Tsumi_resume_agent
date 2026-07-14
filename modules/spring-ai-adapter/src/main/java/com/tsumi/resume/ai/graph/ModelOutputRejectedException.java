package com.tsumi.resume.ai.graph;

public final class ModelOutputRejectedException extends RuntimeException {
    public ModelOutputRejectedException(String node, Throwable cause) {
        super("Structured model output was rejected for " + node, cause);
    }

    public ModelOutputRejectedException(String node, String reason) {
        super("Structured model output was rejected for " + node + ": " + reason);
    }
}
