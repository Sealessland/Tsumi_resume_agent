package com.tsumi.resume.workflow.resume;

public final class ResumeVersionNotFoundException extends RuntimeException {

    public ResumeVersionNotFoundException(String resumeId, long version) {
        super("Resume version not found: " + resumeId + "@" + version);
    }
}
