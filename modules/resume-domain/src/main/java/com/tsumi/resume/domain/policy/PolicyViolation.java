package com.tsumi.resume.domain.policy;

public enum PolicyViolation {
    MISSING_EVIDENCE,
    INCOMPLETE_EVIDENCE_COVERAGE,
    UNSUPPORTED_ATOMIC_CLAIM,
    REMOVE_INTENT_MISMATCH,
    REMOVE_AFTER_NOT_EMPTY
}
