package com.tsumi.resume.domain.policy;

import com.tsumi.resume.domain.patch.PatchIntent;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.patch.PolicyDecision;
import com.tsumi.resume.domain.patch.ResumePatch;
import com.tsumi.resume.domain.patch.ReviewStatus;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public final class PatchPolicy {

    public PolicyEvaluation evaluate(PatchProposal proposal, PatchAssessment assessment) {
        var violations = EnumSet.noneOf(PolicyViolation.class);

        if (proposal.evidenceRefs().isEmpty()) {
            violations.add(PolicyViolation.MISSING_EVIDENCE);
        }
        if (Double.compare(assessment.evidenceCoverage(), 1.0) != 0) {
            violations.add(PolicyViolation.INCOMPLETE_EVIDENCE_COVERAGE);
        }
        if (!assessment.newAtomicClaims().isEmpty()) {
            violations.add(PolicyViolation.UNSUPPORTED_ATOMIC_CLAIM);
        }
        if (proposal.op() == PatchOperation.REMOVE) {
            if (proposal.intent() != PatchIntent.DELETE) {
                violations.add(PolicyViolation.REMOVE_INTENT_MISMATCH);
            }
            if (!proposal.after().isEmpty()) {
                violations.add(PolicyViolation.REMOVE_AFTER_NOT_EMPTY);
            }
        }

        if (!violations.isEmpty()) {
            return new PolicyEvaluation(
                    PolicyDecision.REJECT, List.copyOf(violations), Optional.empty());
        }

        var patch = new ResumePatch(
                proposal.patchId(),
                proposal.taskId(),
                proposal.resumeId(),
                proposal.baseVersion(),
                proposal.op(),
                proposal.path(),
                proposal.before(),
                proposal.after(),
                proposal.intent(),
                proposal.evidenceRefs(),
                proposal.jdRefs(),
                assessment.evidenceCoverage(),
                assessment.newAtomicClaims(),
                proposal.confidence(),
                assessment.riskFlags(),
                PolicyDecision.ALLOW,
                ReviewStatus.PENDING);
        return new PolicyEvaluation(
                PolicyDecision.ALLOW, List.of(), Optional.of(patch));
    }
}
