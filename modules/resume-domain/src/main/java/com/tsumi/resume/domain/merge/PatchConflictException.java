package com.tsumi.resume.domain.merge;

public final class PatchConflictException extends RuntimeException {

    public PatchConflictException(String message) {
        super(message);
    }
}
