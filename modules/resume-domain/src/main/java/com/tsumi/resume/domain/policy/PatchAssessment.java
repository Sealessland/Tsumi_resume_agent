package com.tsumi.resume.domain.policy;

import com.tsumi.resume.domain.evidence.ClaimAssessment;
import java.util.List;

public record PatchAssessment(
        double evidenceCoverage,
        List<String> newAtomicClaims,
        List<String> riskFlags,
        List<ClaimAssessment> claims) {

    public PatchAssessment {
        newAtomicClaims = List.copyOf(newAtomicClaims);
        riskFlags = List.copyOf(riskFlags);
        claims = List.copyOf(claims);
    }

    public PatchAssessment(
            double evidenceCoverage,
            List<String> newAtomicClaims,
            List<String> riskFlags) {
        this(evidenceCoverage, newAtomicClaims, riskFlags, List.of());
    }
}
