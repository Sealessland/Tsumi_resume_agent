package com.tsumi.resume.workflow;

public record CreateTaskCommand(
        String resumeId,
        long baseVersion,
        String jobDescription) {}
