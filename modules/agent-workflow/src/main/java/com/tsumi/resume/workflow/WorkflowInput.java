package com.tsumi.resume.workflow;

public record WorkflowInput(
        String taskId,
        String resumeId,
        long baseVersion,
        String jobDescription) {}
