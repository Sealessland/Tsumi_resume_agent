package com.tsumi.resume.workflow.resume;

public final class DuplicateResumeVersionException extends RuntimeException {

    public DuplicateResumeVersionException(String resumeId, long version) {
        super("Resume version already exists: " + resumeId + "@" + version);
    }
}
