package com.tsumi.resume.server.task;

public final class SseCursorInvalidException extends RuntimeException {
    public SseCursorInvalidException() {
        super("Last-Event-ID does not identify an event in this task stream");
    }
}
