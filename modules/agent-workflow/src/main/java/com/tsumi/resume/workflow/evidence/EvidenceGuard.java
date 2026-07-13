package com.tsumi.resume.workflow.evidence;

import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import com.tsumi.resume.domain.policy.PatchAssessment;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.task.ResumeTask;
import java.util.List;

@FunctionalInterface
public interface EvidenceGuard {
    PatchAssessment assess(
            ResumeTask task,
            PatchProposal proposal,
            List<EvidenceArtifact> approvedEvidence);
}
