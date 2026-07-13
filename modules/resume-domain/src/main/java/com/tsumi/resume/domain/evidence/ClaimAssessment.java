package com.tsumi.resume.domain.evidence;

import java.util.List;
import java.util.Objects;

public record ClaimAssessment(
        String claim,
        ClaimVerdict verdict,
        List<String> evidenceRefs,
        String reason) {

    public ClaimAssessment {
        if (claim == null || claim.isBlank()) throw new IllegalArgumentException("claim must not be blank");
        Objects.requireNonNull(verdict, "verdict");
        evidenceRefs = List.copyOf(evidenceRefs);
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason must not be blank");
    }
}
