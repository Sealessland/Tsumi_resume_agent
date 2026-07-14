package com.tsumi.resume.server.task;

public final class SseConnectionRejectedException extends RuntimeException {
    private final boolean capacity;

    public SseConnectionRejectedException(String message, boolean capacity) {
        super(message);
        this.capacity = capacity;
    }

    public boolean capacity() {
        return capacity;
    }
}
