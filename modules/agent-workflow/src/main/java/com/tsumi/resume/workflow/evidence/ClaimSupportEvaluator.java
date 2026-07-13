package com.tsumi.resume.workflow.evidence;

import com.tsumi.resume.domain.evidence.ClaimAssessment;
import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import java.util.List;

@FunctionalInterface
public interface ClaimSupportEvaluator {
    List<ClaimAssessment> evaluate(
            String before,
            String after,
            List<EvidenceArtifact> evidence);
}
