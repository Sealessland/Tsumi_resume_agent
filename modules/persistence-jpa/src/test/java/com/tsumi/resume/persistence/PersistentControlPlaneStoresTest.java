package com.tsumi.resume.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsumi.resume.domain.patch.PatchIntent;
import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.patch.PolicyDecision;
import com.tsumi.resume.domain.patch.ResumePatch;
import com.tsumi.resume.domain.patch.ReviewStatus;
import com.tsumi.resume.persistence.config.PersistenceJpaConfiguration;
import com.tsumi.resume.task.NewTaskEvent;
import com.tsumi.resume.task.IdempotencyConflictException;
import com.tsumi.resume.task.IdempotencyRecord;
import com.tsumi.resume.task.IdempotencyStore;
import com.tsumi.resume.task.TaskEventStore;
import com.tsumi.resume.task.TaskStatus;
import com.tsumi.resume.task.WorkflowCheckpoint;
import com.tsumi.resume.task.WorkflowCheckpointStore;
import com.tsumi.resume.task.WorkflowNode;
import com.tsumi.resume.workflow.resume.DuplicateResumeVersionException;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import com.tsumi.resume.workflow.resume.ResumeVersionStore;
import com.tsumi.resume.workflow.review.PatchStore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = PersistentControlPlaneStoresTest.TestApplication.class,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:control-stores;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.flyway.enabled=true"
        })
@ActiveProfiles("test")
class PersistentControlPlaneStoresTest {

    @Autowired private ResumeVersionStore resumes;
    @Autowired private PatchStore patches;
    @Autowired private TaskEventStore events;
    @Autowired private WorkflowCheckpointStore checkpoints;
    @Autowired private EvidenceArtifactStore evidence;
    @Autowired private IdempotencyStore idempotency;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void persistsImmutableResumeVersions() {
        var resume = resume("res_store_01", 1);
        resumes.save(resume);

        var restored = resumes.find("res_store_01", 1).orElseThrow();
        assertThat(restored.path("resumeId").asText()).isEqualTo("res_store_01");
        assertThat(restored.path("version").asLong()).isEqualTo(1);
        assertThat(restored.path("basics").path("name").asText()).isEqualTo("Private");
        assertThat(resumes.versions("res_store_01")).containsExactly(1L);
        assertThatThrownBy(() -> resumes.save(resume.deepCopy()))
                .isInstanceOf(DuplicateResumeVersionException.class);
    }

    @Test
    void persistsPatchReviewState() {
        var pending = patch("patch_store_01");
        patches.save(pending);
        patches.save(pending.reviewedAs(ReviewStatus.ACCEPTED));

        assertThat(patches.find("task_store_01", "patch_store_01"))
                .get()
                .extracting(ResumePatch::reviewStatus)
                .isEqualTo(ReviewStatus.ACCEPTED);
    }

    @Test
    void assignsOrderedEventIdsAndReadsAfterCursor() {
        var occurredAt = Instant.parse("2026-07-13T01:00:00Z");
        var first = events.append(new NewTaskEvent(
                "task_store_01", "agent.node.started", TaskStatus.ANALYZING,
                1, "trace_store_01", occurredAt, Map.of("node", "JD_ANALYST")));
        var second = events.append(new NewTaskEvent(
                "task_store_01", "agent.node.completed", TaskStatus.ANALYZING,
                1, "trace_store_01", occurredAt.plusSeconds(1), Map.of("tokens", "42")));

        assertThat(second.eventId()).isGreaterThan(first.eventId());
        assertThat(events.findAfter("task_store_01", first.eventId(), 10))
                .containsExactly(second);
    }

    @Test
    void restoresLatestCheckpointForNode() {
        var now = Instant.parse("2026-07-13T02:00:00Z");
        checkpoints.save(WorkflowCheckpoint.started(
                "task_store_01", WorkflowNode.JD_ANALYST, 1, "input-1", now));
        var completed = WorkflowCheckpoint.completed(
                "task_store_01", WorkflowNode.JD_ANALYST, 1,
                "input-1", "output-1", now.plusSeconds(1));
        checkpoints.save(completed);

        assertThat(checkpoints.findLatest("task_store_01", WorkflowNode.JD_ANALYST))
                .contains(completed);
        assertThat(checkpoints.findByTaskId("task_store_01")).hasSize(2);
    }

    @Test
    void persistsEvidenceWithAuthorizationAndHash() {
        var now = Instant.parse("2026-07-13T03:00:00Z");
        var artifact = EvidenceArtifact.approved(
                "ev_store_01", "task_store_01", "res_store_01", "resume", "resume:/projects/0",
                "Built an export workflow", now.plusSeconds(60), now);
        evidence.save(artifact);

        assertThat(evidence.findById("ev_store_01")).contains(artifact);
        assertThat(evidence.findByTaskId("task_store_01")).contains(artifact);
    }

    @Test
    void idempotencyKeyReturnsOriginalResponseAndRejectsDifferentHash() {
        var now = Instant.parse("2026-07-13T03:30:00Z");
        var original = new IdempotencyRecord(
                "POST:/api/v1/tasks", "idem-01", "hash-a", 202, "{\"taskId\":\"task-1\"}", now);
        assertThat(idempotency.save(original)).isEqualTo(original);
        assertThat(idempotency.save(original)).isEqualTo(original);
        assertThatThrownBy(() -> idempotency.save(new IdempotencyRecord(
                        original.scope(), original.key(), "hash-b", 202, "{}", now)))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    private ObjectNode resume(String resumeId, long version) {
        var root = objectMapper.createObjectNode();
        root.put("schemaVersion", 13);
        root.put("resumeId", resumeId);
        root.put("version", version);
        root.putObject("basics").put("name", "Private");
        return root;
    }

    private ResumePatch patch(String patchId) {
        return new ResumePatch(
                patchId, "task_store_01", "res_store_01", 1,
                PatchOperation.REPLACE, "/basics/summary", "before", "after",
                PatchIntent.PARAPHRASE, List.of("ev_01"), List.of("jd_01"),
                1.0, List.of(), 0.8, List.of(),
                PolicyDecision.ALLOW, ReviewStatus.PENDING);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(PersistenceJpaConfiguration.class)
    static class TestApplication {}
}
