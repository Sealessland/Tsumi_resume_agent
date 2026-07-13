package com.tsumi.resume.server.api;

import com.tsumi.resume.domain.policy.PolicyViolation;
import java.util.List;

public final class PolicyRejectedException extends RuntimeException {

    private final List<PolicyViolation> violations;

    public PolicyRejectedException(List<PolicyViolation> violations) {
        super("Patch proposal was rejected by the evidence policy");
        this.violations = List.copyOf(violations);
    }

    public List<PolicyViolation> violations() {
        return violations;
    }
}
