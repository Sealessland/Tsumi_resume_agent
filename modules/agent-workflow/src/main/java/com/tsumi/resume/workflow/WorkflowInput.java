package com.tsumi.resume.workflow;

public record WorkflowInput(
        String taskId,
        String resumeId,
        long baseVersion,
        String jobDescription,
        int repairCount) {

    public WorkflowInput(String taskId, String resumeId, long baseVersion, String jobDescription) {
        this(taskId, resumeId, baseVersion, jobDescription, 0);
    }

    public WorkflowInput {
        if (repairCount < 0 || repairCount > 1) {
            throw new IllegalArgumentException("repairCount must be zero or one");
        }
    }
}
