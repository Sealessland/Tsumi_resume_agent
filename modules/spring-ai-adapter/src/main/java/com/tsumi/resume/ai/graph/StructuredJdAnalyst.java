package com.tsumi.resume.ai.graph;

@FunctionalInterface
public interface StructuredJdAnalyst {
    CapabilityMatrix analyze(String jobDescription);
}
