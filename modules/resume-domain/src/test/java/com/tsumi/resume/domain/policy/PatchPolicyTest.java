package com.tsumi.resume.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.tsumi.resume.domain.patch.PatchIntent;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.patch.PolicyDecision;
import com.tsumi.resume.domain.patch.ReviewStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class PatchPolicyTest {

    private final PatchPolicy policy = new PatchPolicy();

    @Test
    void createsAServerOwnedPendingPatchForFullySupportedProposal() {
        var evaluation = policy.evaluate(validProposal(), supportedAssessment());

        assertThat(evaluation.decision()).isEqualTo(PolicyDecision.ALLOW);
        assertThat(evaluation.violations()).isEmpty();
        assertThat(evaluation.patch()).isPresent();
        var patch = evaluation.patch().orElseThrow();
        assertThat(patch.policyDecision()).isEqualTo(PolicyDecision.ALLOW);
        assertThat(patch.reviewStatus()).isEqualTo(ReviewStatus.PENDING);
        assertThat(patch.evidenceCoverage()).isEqualTo(1.0);

        var accepted = patch.reviewedAs(ReviewStatus.ACCEPTED);
        assertThat(accepted.reviewStatus()).isEqualTo(ReviewStatus.ACCEPTED);
        assertThatIllegalStateException()
                .isThrownBy(() -> accepted.reviewedAs(ReviewStatus.REJECTED));
    }

    @Test
    void rejectsUnsupportedAtomicClaimRegardlessOfHighConfidence() {
        var proposal = new PatchProposal(
                "rp_01", "task_01", "res_01", 1, PatchOperation.REPLACE,
                "/projects/project_01/description", "before", "降低耗时 50%",
                PatchIntent.PARAPHRASE, List.of("resume:project_01"), List.of(), 0.99);
        var assessment = new PatchAssessment(
                0.5, List.of("降低耗时 50%"), List.of("UNSUPPORTED_METRIC"));

        var evaluation = policy.evaluate(proposal, assessment);

        assertThat(evaluation.decision()).isEqualTo(PolicyDecision.REJECT);
        assertThat(evaluation.patch()).isEmpty();
        assertThat(evaluation.violations()).contains(
                PolicyViolation.INCOMPLETE_EVIDENCE_COVERAGE,
                PolicyViolation.UNSUPPORTED_ATOMIC_CLAIM);
    }

    @Test
    void rejectsProposalWithoutEvidence() {
        var proposal = new PatchProposal(
                "rp_01", "task_01", "res_01", 1, PatchOperation.REPLACE,
                "/profile/title", "Java", "Java Agent Engineer",
                PatchIntent.PARAPHRASE, List.of(), List.of(), 0.9);

        var evaluation = policy.evaluate(proposal, supportedAssessment());

        assertThat(evaluation.violations()).containsExactly(PolicyViolation.MISSING_EVIDENCE);
        assertThat(evaluation.patch()).isEmpty();
    }

    @Test
    void rejectsRemoveProposalThatIsNotAnEmptyDelete() {
        var proposal = new PatchProposal(
                "rp_01", "task_01", "res_01", 1, PatchOperation.REMOVE,
                "/profile/title", "Java", "replacement",
                PatchIntent.PARAPHRASE, List.of("resume:profile"), List.of(), 0.9);

        var evaluation = policy.evaluate(proposal, supportedAssessment());

        assertThat(evaluation.violations()).containsExactlyInAnyOrder(
                PolicyViolation.REMOVE_INTENT_MISMATCH,
                PolicyViolation.REMOVE_AFTER_NOT_EMPTY);
        assertThat(evaluation.patch()).isEmpty();
    }

    private PatchProposal validProposal() {
        return new PatchProposal(
                "rp_01", "task_01", "res_01", 1, PatchOperation.REPLACE,
                "/projects/project_01/description", "before", "after",
                PatchIntent.PARAPHRASE,
                List.of("resume:projects/project_01"),
                List.of("jd:delivery/export"),
                0.92);
    }

    private PatchAssessment supportedAssessment() {
        return new PatchAssessment(1.0, List.of(), List.of());
    }
}
