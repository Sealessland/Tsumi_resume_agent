package com.tsumi.resume.workflow.review;

import com.tsumi.resume.domain.evidence.ClaimAssessment;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.patch.PolicyDecision;
import com.tsumi.resume.domain.patch.ReviewStatus;
import java.util.List;

public record PatchReviewItem(
        String patchId,
        String path,
        PatchOperation op,
        String before,
        String after,
        List<EvidenceView> evidence,
        List<ClaimAssessment> claims,
        PolicyDecision policyDecision,
        List<String> risks,
        ReviewStatus reviewStatus,
        List<AllowedAction> actions,
        long expectedBaseVersion) {

    public PatchReviewItem {
        evidence = List.copyOf(evidence);
        claims = List.copyOf(claims);
        risks = List.copyOf(risks);
        actions = List.copyOf(actions);
    }
}
