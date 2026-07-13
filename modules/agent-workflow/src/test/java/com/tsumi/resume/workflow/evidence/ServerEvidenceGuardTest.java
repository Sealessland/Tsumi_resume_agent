package com.tsumi.resume.workflow.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsumi.resume.domain.evidence.ClaimAssessment;
import com.tsumi.resume.domain.evidence.ClaimVerdict;
import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import com.tsumi.resume.domain.patch.PatchIntent;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.task.ResumeTask;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServerEvidenceGuardTest {

    private static final Instant NOW = Instant.parse("2026-07-13T04:00:00Z");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResumeTask task = ResumeTask.created(
            "task_guard_01", "res_guard_01", 1, "trace_guard_01", NOW);

    @Test
    void serverComputesCoverageFromClaimVerdicts() {
        var evidence = evidence("ev_guard_01", "Built a resilient export workflow");
        var guard = guard((before, after, artifacts) -> List.of(new ClaimAssessment(
                after, ClaimVerdict.SUPPORTED, List.of("ev_guard_01"), "Directly supported")));

        var assessment = guard.assess(task, proposal("Built a resilient export workflow"), List.of(evidence));

        assertThat(assessment.evidenceCoverage()).isEqualTo(1.0);
        assertThat(assessment.newAtomicClaims()).isEmpty();
        assertThat(assessment.claims()).allMatch(claim -> claim.verdict() == ClaimVerdict.SUPPORTED);
    }

    @Test
    void rejectsNewMetricEvenWhenModelEvaluatorClaimsSupport() {
        var evidence = evidence("ev_guard_01", "Improved the export workflow");
        var guard = guard((before, after, artifacts) -> List.of(new ClaimAssessment(
                after, ClaimVerdict.SUPPORTED, List.of("ev_guard_01"), "Model said supported")));

        var assessment = guard.assess(task, proposal("Export performance improved by 50%"), List.of(evidence));

        assertThat(assessment.evidenceCoverage()).isLessThan(1.0);
        assertThat(assessment.newAtomicClaims()).contains("50%");
        assertThat(assessment.claims()).anyMatch(claim -> claim.verdict() == ClaimVerdict.UNSUPPORTED);
    }

    @Test
    void rejectsExpiredOrHashInvalidEvidence() {
        var expired = EvidenceArtifact.approved(
                "ev_guard_01", task.taskId(), task.resumeId(), "resume", "resume:/work/0",
                "Built a resilient export workflow", NOW.minusSeconds(1), NOW.minusSeconds(60));

        assertThatThrownBy(() -> guard((before, after, artifacts) -> List.of())
                        .assess(task, proposal("Built a resilient export workflow"), List.of(expired)))
                .isInstanceOf(EvidenceNotApprovedException.class);
    }

    @Test
    void rejectsProposalWhoseBeforeDoesNotMatchBaseResume() {
        var evidence = evidence("ev_guard_01", "Built a resilient export workflow");
        var mismatched = new PatchProposal(
                "patch_guard_01", task.taskId(), task.resumeId(), task.baseVersion(),
                PatchOperation.REPLACE, "/basics/summary", "forged before", "after",
                PatchIntent.PARAPHRASE, List.of("ev_guard_01"), List.of(), 0.8);

        assertThatThrownBy(() -> guard((before, after, artifacts) -> List.of())
                        .assess(task, mismatched, List.of(evidence)))
                .isInstanceOf(ProposalBaseMismatchException.class);
    }

    private ServerEvidenceGuard guard(ClaimSupportEvaluator evaluator) {
        return new ServerEvidenceGuard(
                (resumeId, version) -> {
                    var root = objectMapper.createObjectNode();
                    root.put("resumeId", resumeId);
                    root.put("version", version);
                    root.putObject("basics").put("summary", "Built export workflow");
                    return root;
                },
                evaluator,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private PatchProposal proposal(String after) {
        return new PatchProposal(
                "patch_guard_01", task.taskId(), task.resumeId(), task.baseVersion(),
                PatchOperation.REPLACE, "/basics/summary", "Built export workflow", after,
                PatchIntent.PARAPHRASE, List.of("ev_guard_01"), List.of(), 0.8);
    }

    private EvidenceArtifact evidence(String id, String excerpt) {
        return EvidenceArtifact.approved(
                id, task.taskId(), task.resumeId(), "resume", "resume:/work/0",
                excerpt, NOW.plusSeconds(3600), NOW);
    }
}
