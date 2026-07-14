package com.tsumi.resume.ai.graph;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsumi.resume.domain.evidence.ClaimAssessment;
import com.tsumi.resume.domain.evidence.ClaimVerdict;
import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import com.tsumi.resume.domain.patch.PatchIntent;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.policy.PatchAssessment;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.workflow.WorkflowInput;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class EvidenceWorkflowAdaptersTest {

    @Test
    void deterministicPrecheckTurnsModelTargetDriftIntoNonActionableGap() throws Exception {
        var content = (com.fasterxml.jackson.databind.node.ObjectNode) new ObjectMapper().readTree("""
                {"schemaVersion":13,"resumeId":"resume_01","version":1,
                "basics":{"summary":"负责后端开发"}}
                """);
        var input = new WorkflowInput("task_01", "resume_01", 1, "Java");
        var invalid = new PatchProposal(
                "patch_01", "another_task", "resume_01", 1, PatchOperation.REPLACE,
                "/basics/summary", "不存在的原文", "性能提升 50%", PatchIntent.PARAPHRASE,
                List.of("evidence_01"), List.of("jd_01"), 0.9);

        var result = new DeterministicProposalPreChecker().check(
                input, new ResumeModelView("resume_01", 1, content), List.of(invalid));

        assertThat(result.candidates()).isEmpty();
        assertThat(result.gaps()).singleElement().satisfies(gap -> {
            assertThat(gap.patchId()).isEqualTo("patch_01");
            assertThat(gap.reason()).contains("target");
        });
    }

    @Test
    void serverVerifierNeverTreatsModelCoverageAsAuthoritative() {
        var now = Instant.parse("2026-07-14T00:00:00Z");
        var task = ResumeTask.created("task_01", "resume_01", 1, "trace_01", now);
        var proposal = proposal("性能提升 50%");
        var tasks = new FixedTaskRepository(task);
        var evidence = EvidenceArtifact.approved(
                "evidence_01", "task_01", "resume_01", "user_note", "note_01",
                "负责后端开发", now.plusSeconds(60), now);
        var evidenceStore = new FixedEvidenceStore(List.of(evidence));
        var guardCalls = new java.util.concurrent.atomic.AtomicInteger();
        var verifier = new ServerWorkflowEvidenceVerifier(
                tasks,
                evidenceStore,
                (serverTask, serverProposal, approvedEvidence) -> {
                    guardCalls.incrementAndGet();
                    return new PatchAssessment(
                            0.0, List.of("50%"), List.of("UNSUPPORTED_PROTECTED_FACT"),
                            List.of(new ClaimAssessment(
                                    "50%", ClaimVerdict.UNSUPPORTED, List.of(), "Evidence 中不存在")));
                });

        var result = verifier.verify(
                new WorkflowInput("task_01", "resume_01", 1, "Java"), List.of(proposal));

        assertThat(guardCalls).hasValue(1);
        assertThat(result.supported()).isEmpty();
        assertThat(result.gaps()).singleElement().satisfies(gap ->
                assertThat(gap.unsupportedClaims()).containsExactly("50%"));
    }

    @Test
    void dashScopeGuardReturnsClaimVerdictsButNotFinalCoverage() {
        ChatModel model = prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage("""
                {"claims":[{"claim":"使用 Spring Boot","verdict":"SUPPORTED",
                "evidenceRefs":["evidence_01"],"reason":"原文明确出现"}]}
                """))));
        var client = new DashScopeStructuredModelClient(
                model, new ObjectMapper(), Duration.ofSeconds(1), 1_000);
        var evaluator = new DashScopeClaimSupportEvaluator(client, "qwen-plus");
        var now = Instant.parse("2026-07-14T00:00:00Z");
        var evidence = EvidenceArtifact.approved(
                "evidence_01", "task_01", "resume_01", "user_note", "note_01",
                "项目使用 Spring Boot", now.plusSeconds(60), now);

        var claims = evaluator.evaluate(
                "负责后端开发", "使用 Spring Boot 负责后端开发", List.of(evidence));

        assertThat(claims).singleElement().satisfies(claim -> {
            assertThat(claim.verdict()).isEqualTo(ClaimVerdict.SUPPORTED);
            assertThat(claim.evidenceRefs()).containsExactly("evidence_01");
        });
    }

    private PatchProposal proposal(String after) {
        return new PatchProposal(
                "patch_01", "task_01", "resume_01", 1, PatchOperation.REPLACE,
                "/basics/summary", "负责后端开发", after, PatchIntent.PARAPHRASE,
                List.of("evidence_01"), List.of("jd_01"), 0.9);
    }

    private static final class FixedTaskRepository implements TaskRepository {
        private final ResumeTask task;
        private FixedTaskRepository(ResumeTask task) { this.task = task; }
        @Override public ResumeTask save(ResumeTask task) { return task; }
        @Override public Optional<ResumeTask> findById(String taskId) { return Optional.of(task); }
    }

    private static final class FixedEvidenceStore implements EvidenceArtifactStore {
        private final List<EvidenceArtifact> artifacts;
        private FixedEvidenceStore(List<EvidenceArtifact> artifacts) { this.artifacts = artifacts; }
        @Override public EvidenceArtifact save(EvidenceArtifact artifact) { return artifact; }
        @Override public Optional<EvidenceArtifact> findById(String artifactId) { return artifacts.stream().filter(a -> a.artifactId().equals(artifactId)).findFirst(); }
        @Override public List<EvidenceArtifact> findByTaskId(String taskId) { return artifacts; }
    }
}
