package com.tsumi.resume.workflow.review;

public final class NoAcceptedPatchesException extends RuntimeException {

    public NoAcceptedPatchesException(String taskId) {
        super("No accepted patches are available for task: " + taskId);
    }
}
