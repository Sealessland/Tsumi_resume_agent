package com.tsumi.resume.workflow;

public class WorkflowExecutionException extends RuntimeException {
    private final String code;
    private final boolean retryable;

    public WorkflowExecutionException(String code, boolean retryable, String message) {
        this(code, retryable, message, null);
    }

    public WorkflowExecutionException(String code, boolean retryable, String message, Throwable cause) {
        super(message, cause);
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code must not be blank");
        this.code = code;
        this.retryable = retryable;
    }

    public String code() { return code; }
    public boolean retryable() { return retryable; }
}
