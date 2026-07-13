package com.tsumi.resume.domain.policy;

import com.tsumi.resume.domain.patch.PolicyDecision;
import com.tsumi.resume.domain.patch.ResumePatch;
import java.util.List;
import java.util.Optional;

public record PolicyEvaluation(
        PolicyDecision decision,
        List<PolicyViolation> violations,
        Optional<ResumePatch> patch) {

    public PolicyEvaluation {
        violations = List.copyOf(violations);
    }
}
