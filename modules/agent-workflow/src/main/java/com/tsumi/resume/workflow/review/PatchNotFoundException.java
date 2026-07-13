package com.tsumi.resume.workflow.review;

public final class PatchNotFoundException extends RuntimeException {

    public PatchNotFoundException(String taskId, String patchId) {
        super("Patch not found: " + taskId + "/" + patchId);
    }
}
