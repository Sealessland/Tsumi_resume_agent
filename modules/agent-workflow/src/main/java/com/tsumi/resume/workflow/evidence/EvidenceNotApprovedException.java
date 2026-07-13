package com.tsumi.resume.workflow.evidence;

public final class EvidenceNotApprovedException extends RuntimeException {
    public EvidenceNotApprovedException(String artifactId) {
        super("Evidence is missing, unapproved, expired, or hash-invalid: " + artifactId);
    }
}
