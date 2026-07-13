package com.tsumi.resume.domain.merge;

public final class VersionConflictException extends RuntimeException {

    public VersionConflictException(long expected, long actual) {
        super("Resume version conflict: expected " + expected + " but was " + actual);
    }
}
