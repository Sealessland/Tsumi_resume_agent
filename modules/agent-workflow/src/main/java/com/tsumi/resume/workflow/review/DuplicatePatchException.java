package com.tsumi.resume.workflow.review;

public final class DuplicatePatchException extends RuntimeException {

    public DuplicatePatchException(String taskId, String patchId) {
        super("Patch already exists: " + taskId + "/" + patchId);
    }
}
