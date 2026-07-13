package com.tsumi.resume.domain.policy;

import java.util.List;

public record PatchAssessment(
        double evidenceCoverage,
        List<String> newAtomicClaims,
        List<String> riskFlags) {

    public PatchAssessment {
        newAtomicClaims = List.copyOf(newAtomicClaims);
        riskFlags = List.copyOf(riskFlags);
    }
}
