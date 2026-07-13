package com.tsumi.resume.workflow.evidence;

public final class ProposalBaseMismatchException extends RuntimeException {
    public ProposalBaseMismatchException(String path) {
        super("Proposal before value does not match base Resume at " + path);
    }
}
