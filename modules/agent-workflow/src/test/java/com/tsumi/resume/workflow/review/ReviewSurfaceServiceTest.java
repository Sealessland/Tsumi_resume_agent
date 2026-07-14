package com.tsumi.resume.workflow.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.tsumi.resume.domain.evidence.ClaimAssessment;
import com.tsumi.resume.domain.evidence.ClaimVerdict;
import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import com.tsumi.resume.domain.patch.PatchIntent;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.patch.PolicyDecision;
import com.tsumi.resume.domain.patch.ResumePatch;
import com.tsumi.resume.domain.patch.ReviewStatus;
import com.tsumi.resume.task.NewTaskEvent;
import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskEvent;
import com.tsumi.resume.task.TaskEventStore;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.task.TaskStatus;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReviewSurfaceServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-14T06:00:00Z");

    @Test
    void buildsEvidenceBackedDiffActionsGapsAndTimelineWithoutInventingCost() {
        var task = ResumeTask.created("task_surface_01", "res_surface_01", 1, "trace_surface_01", NOW)
                .analyze(NOW).propose(NOW).verify(NOW).reviewReady("ready", NOW);
        var claim = new ClaimAssessment(
                "负责 Java 服务开发", ClaimVerdict.SUPPORTED, List.of("ev_surface_01"),
                "The approved resume excerpt supports the responsibility");
        var patch = new ResumePatch(
                "rp_surface_01", task.taskId(), task.resumeId(), task.baseVersion(),
                PatchOperation.REPLACE, "/projects/project_01/description",
                "开发 Java 服务", "负责 Java 服务开发", PatchIntent.PARAPHRASE,
                List.of("ev_surface_01"), List.of("jd_java"), 1.0, List.of(), 0.8,
                List.of(), List.of(claim), PolicyDecision.ALLOW, ReviewStatus.PENDING);
        var evidence = EvidenceArtifact.approved(
                "ev_surface_01", task.taskId(), task.resumeId(), "resume",
                "resume:/projects/project_01/description", "开发 Java 服务", null, NOW);
        var events = new MemoryEvents();
        events.append(new NewTaskEvent(task.taskId(), "task.review.ready", TaskStatus.REVIEW_READY,
                1, task.traceId(), NOW, Map.of("reviewReady", "true")));
        var service = new ReviewSurfaceService(
                new FixedTaskRepository(task),
                new FixedPatchStore(patch),
                new FixedEvidenceStore(evidence),
                events,
                new FixedGapStore(List.of(new CoverageGap(
                        "rp_blocked", "/projects/project_02/description",
                        List.of("性能提升 50%"), "Unsupported protected fact"))));

        var surface = service.get(task.taskId());

        assertThat(surface.surfaceId()).isEqualTo("review_task_surface_01_v1");
        assertThat(surface.patches()).singleElement().satisfies(item -> {
            assertThat(item.before()).isEqualTo("开发 Java 服务");
            assertThat(item.after()).isEqualTo("负责 Java 服务开发");
            assertThat(item.evidence()).singleElement().satisfies(view -> {
                assertThat(view.artifactId()).isEqualTo("ev_surface_01");
                assertThat(view.contentHash()).isEqualTo(evidence.contentHash());
            });
            assertThat(item.claims()).containsExactly(claim);
            assertThat(item.actions()).containsExactly(
                    AllowedAction.ACCEPT, AllowedAction.REJECT, AllowedAction.EDIT);
        });
        assertThat(surface.gaps()).singleElement()
                .extracting(CoverageGap::unsupportedClaims)
                .isEqualTo(List.of("性能提升 50%"));
        assertThat(surface.timeline()).singleElement()
                .extracting(TaskTimelineItem::type)
                .isEqualTo("task.review.ready");
        assertThat(surface.costSummary().estimatedCost()).isNull();
        assertThat(surface.costSummary().inputTokens()).isNull();
        assertThat(surface.actions()).contains(AllowedAction.CANCEL);
    }

    private record FixedPatchStore(ResumePatch patch) implements PatchStore {
        @Override public ResumePatch save(ResumePatch value) { return value; }
        @Override public Optional<ResumePatch> find(String taskId, String patchId) {
            return patch.taskId().equals(taskId) && patch.patchId().equals(patchId)
                    ? Optional.of(patch) : Optional.empty();
        }
        @Override public List<ResumePatch> findByTaskId(String taskId) {
            return patch.taskId().equals(taskId) ? List.of(patch) : List.of();
        }
    }

    private record FixedTaskRepository(ResumeTask task) implements TaskRepository {
        @Override public ResumeTask save(ResumeTask value) { return value; }
        @Override public Optional<ResumeTask> findById(String taskId) {
            return task.taskId().equals(taskId) ? Optional.of(task) : Optional.empty();
        }
    }

    private record FixedGapStore(List<CoverageGap> gaps) implements CoverageGapStore {
        @Override public void replace(String taskId, List<CoverageGap> values) {}
        @Override public List<CoverageGap> findByTaskId(String taskId) { return gaps; }
    }

    private record FixedEvidenceStore(EvidenceArtifact evidence) implements EvidenceArtifactStore {
        @Override public EvidenceArtifact save(EvidenceArtifact artifact) { return artifact; }
        @Override public Optional<EvidenceArtifact> findById(String artifactId) {
            return evidence.artifactId().equals(artifactId) ? Optional.of(evidence) : Optional.empty();
        }
        @Override public List<EvidenceArtifact> findByTaskId(String taskId) {
            return evidence.taskId().equals(taskId) ? List.of(evidence) : List.of();
        }
    }

    private static final class MemoryEvents implements TaskEventStore {
        private final List<TaskEvent> values = new ArrayList<>();
        @Override public TaskEvent append(NewTaskEvent event) {
            var saved = new TaskEvent(values.size() + 1L, event.taskId(), event.type(), event.stage(),
                    event.attempt(), event.traceId(), event.occurredAt(), event.data());
            values.add(saved);
            return saved;
        }
        @Override public List<TaskEvent> findAfter(String taskId, long afterExclusive, int limit) {
            return values.stream().filter(event -> event.taskId().equals(taskId))
                    .filter(event -> event.eventId() > afterExclusive).limit(limit).toList();
        }
    }
}
