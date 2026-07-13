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

    public ResumePatch reviewedAs(ReviewStatus decision) {
        if (reviewStatus != ReviewStatus.PENDING) {
            throw new IllegalStateException("Patch review is already final: " + reviewStatus);
        }
        if (decision == ReviewStatus.PENDING) {
            throw new IllegalArgumentException("Review decision cannot remain pending");
        }
        return new ResumePatch(
                patchId,
                taskId,
                resumeId,
                baseVersion,
                op,
                path,
                before,
                after,
                intent,
                evidenceRefs,
                jdRefs,
                evidenceCoverage,
                newAtomicClaims,
                confidence,
                riskFlags,
                policyDecision,
                decision);
    }
}
