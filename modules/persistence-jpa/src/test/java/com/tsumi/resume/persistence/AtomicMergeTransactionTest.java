package com.tsumi.resume.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsumi.resume.domain.evidence.ClaimAssessment;
import com.tsumi.resume.domain.evidence.ClaimVerdict;
import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import com.tsumi.resume.domain.patch.PatchIntent;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.patch.ReviewStatus;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.persistence.config.PersistenceJpaConfiguration;
import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.task.TaskStatus;
import com.tsumi.resume.workflow.UnitOfWork;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import com.tsumi.resume.workflow.evidence.ServerEvidenceGuard;
import com.tsumi.resume.workflow.resume.ResumeVersionStore;
import com.tsumi.resume.workflow.resume.VersionedResumeService;
import com.tsumi.resume.workflow.review.PatchStore;
import com.tsumi.resume.workflow.review.ResumeReviewService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = AtomicMergeTransactionTest.TestApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:atomic-merge;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"})
class AtomicMergeTransactionTest {
    private static final Instant NOW = Instant.parse("2026-07-13T05:00:00Z");
    @Autowired TaskRepository tasks;
    @Autowired ResumeVersionStore resumes;
    @Autowired PatchStore patches;
    @Autowired EvidenceArtifactStore evidence;
    @Autowired UnitOfWork unitOfWork;
    @Autowired ObjectMapper json;

    @Test void rollsBackResumeAndTaskWhenFinalSaveFails() {
        var created = ResumeTask.created("task_tx_01", "res_tx_01", 1, "trace_tx_01", NOW);
        var ready = created.analyze(NOW).propose(NOW).verify(NOW).reviewReady("ready", NOW);
        tasks.save(created); tasks.save(ready);
        var root = json.createObjectNode();
        root.put("resumeId", "res_tx_01").put("version", 1).put("schemaVersion", 13);
        root.putObject("basics").put("summary", "Built export workflow");
        resumes.save(root);
        evidence.save(EvidenceArtifact.approved("ev_tx_01", ready.taskId(), ready.resumeId(),
                "resume", "resume:/basics/summary", "Built export workflow", null, NOW));
        var versioned = new VersionedResumeService(resumes);
        var guard = new ServerEvidenceGuard(versioned,
                (before, after, artifacts) -> List.of(new ClaimAssessment(after,
                        ClaimVerdict.SUPPORTED, List.of("ev_tx_01"), "supported")),
                Clock.fixed(NOW, ZoneOffset.UTC));
        TaskRepository failing = new TaskRepository() {
            public ResumeTask save(ResumeTask task) {
                var saved = tasks.save(task);
                if (task.status() == TaskStatus.COMPLETED) throw new IllegalStateException("injected failure");
                return saved;
            }
            public Optional<ResumeTask> findById(String id) { return tasks.findById(id); }
        };
        var service = new ResumeReviewService(failing, patches, versioned,
                Clock.fixed(NOW, ZoneOffset.UTC), evidence, guard, unitOfWork);
        var proposal = new PatchProposal("patch_tx_01", ready.taskId(), ready.resumeId(), 1,
                PatchOperation.REPLACE, "/basics/summary", "Built export workflow",
                "Completed export workflow", PatchIntent.PARAPHRASE,
                List.of("ev_tx_01"), List.of(), 0.8);
        service.submit(ready.taskId(), proposal);
        service.decide(ready.taskId(), proposal.patchId(), 1, ReviewStatus.ACCEPTED);

        assertThatThrownBy(() -> service.merge(ready.taskId(), 1)).hasMessage("injected failure");
        assertThat(resumes.versions(ready.resumeId())).containsExactly(1L);
        assertThat(tasks.findById(ready.taskId()).orElseThrow().status()).isEqualTo(TaskStatus.REVIEW_READY);
    }

    @SpringBootConfiguration @EnableAutoConfiguration @Import(PersistenceJpaConfiguration.class)
    static class TestApplication {}
}
