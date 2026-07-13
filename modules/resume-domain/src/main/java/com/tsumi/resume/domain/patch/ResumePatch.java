package com.tsumi.resume.domain.patch;

import java.util.List;

public record ResumePatch(
        String patchId,
        String taskId,
        String resumeId,
        long baseVersion,
        PatchOperation op,
        String path,
        String before,
        String after,
        PatchIntent intent,
        List<String> evidenceRefs,
        List<String> jdRefs,
        double evidenceCoverage,
        List<String> newAtomicClaims,
        double confidence,
        List<String> riskFlags,
        PolicyDecision policyDecision,
        ReviewStatus reviewStatus) {

    public ResumePatch {
        evidenceRefs = List.copyOf(evidenceRefs);
        jdRefs = List.copyOf(jdRefs);
        newAtomicClaims = List.copyOf(newAtomicClaims);
        riskFlags = List.copyOf(riskFlags);
    }
}
