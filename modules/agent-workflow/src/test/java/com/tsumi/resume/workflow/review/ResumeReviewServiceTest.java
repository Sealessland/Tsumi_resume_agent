package com.tsumi.resume.workflow.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsumi.resume.domain.merge.VersionConflictException;
import com.tsumi.resume.domain.patch.PatchIntent;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.patch.PolicyDecision;
import com.tsumi.resume.domain.patch.ResumePatch;
import com.tsumi.resume.domain.patch.ReviewStatus;
import com.tsumi.resume.domain.policy.PatchAssessment;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.task.TaskStatus;
import com.tsumi.resume.workflow.resume.ResumeVersionStore;
import com.tsumi.resume.workflow.resume.VersionedResumeService;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResumeReviewServiceTest {

    private final Path contracts = Path.of(System.getProperty("contracts.dir"));
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MemoryResumeStore resumeStore = new MemoryResumeStore();
    private final MemoryPatchStore patchStore = new MemoryPatchStore();
    private final MemoryTaskRepository taskRepository = new MemoryTaskRepository();
    private PatchAssessment guardAssessment;
    private ResumeReviewService service;

    @BeforeEach
    void setUp() throws Exception {
        var now = Instant.parse("2026-07-13T00:00:00Z");
        taskRepository.save(ResumeTask.created(
                        "task_01", "res_fixture", 1, "trace_01", now)
                .analyze(now)
                .propose(now)
                .verify(now)
                .reviewReady("ready", now));
        resumeStore.save((ObjectNode) objectMapper.readTree(
                contracts.resolve("fixtures/resume/valid-minimal-v13.json").toFile()));
        guardAssessment = new PatchAssessment(1.0, List.of(), List.of());
        service = new ResumeReviewService(
                taskRepository,
                patchStore,
                new VersionedResumeService(resumeStore),
                Clock.fixed(Instant.parse("2026-07-13T00:00:05Z"), ZoneOffset.UTC),
                new EmptyEvidenceStore(),
                (task, proposal, evidence) -> guardAssessment);
    }

    @Test
    void rejectedPolicyEvaluationNeverEntersPatchStore() {
        guardAssessment = new PatchAssessment(0.5, List.of("降低耗时 50%"),
                List.of("UNSUPPORTED_METRIC"));
        var result = service.submit("task_01", proposal());

        assertThat(result.decision()).isEqualTo(PolicyDecision.REJECT);
        assertThat(patchStore.findByTaskId("task_01")).isEmpty();
    }

    @Test
    void acceptedHumanDecisionMergesANewVersionAndCompletesTask() {
        service.submit("task_01", proposal());
        service.decide("task_01", "rp_01", 1, ReviewStatus.ACCEPTED);

        var merged = service.merge("task_01", 1);

        assertThat(merged.path("version").asLong()).isEqualTo(2);
        assertThat(merged.at("/projects/0/description").asText())
                .isEqualTo("打通结构化编辑、实时预览及 PDF/PNG 导出链路。");
        assertThat(resumeStore.versions("res_fixture")).containsExactly(1L, 2L);
        assertThat(taskRepository.findById("task_01").orElseThrow().status())
                .isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void staleExpectedVersionCannotDecideOrMerge() {
        service.submit("task_01", proposal());

        assertThatThrownBy(() -> service.decide(
                "task_01", "rp_01", 2, ReviewStatus.ACCEPTED))
                .isInstanceOf(VersionConflictException.class);
        assertThatThrownBy(() -> service.merge("task_01", 2))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void rejectedPatchCannotProduceAMergeVersion() {
        service.submit("task_01", proposal());
        service.decide("task_01", "rp_01", 1, ReviewStatus.REJECTED);

        assertThatThrownBy(() -> service.merge("task_01", 1))
                .isInstanceOf(NoAcceptedPatchesException.class);
        assertThat(resumeStore.versions("res_fixture")).containsExactly(1L);
    }

    private PatchProposal proposal() {
        return new PatchProposal(
                "rp_01", "task_01", "res_fixture", 1,
                PatchOperation.REPLACE, "/projects/project_01/description",
                "实现简历编辑和导出功能。",
                "打通结构化编辑、实时预览及 PDF/PNG 导出链路。",
                PatchIntent.PARAPHRASE,
                List.of("resume:projects/project_01"),
                List.of("jd:delivery/export"), 0.92);
    }

    private static final class MemoryTaskRepository implements TaskRepository {
        private final Map<String, ResumeTask> tasks = new LinkedHashMap<>();

        @Override
        public ResumeTask save(ResumeTask task) {
            tasks.put(task.taskId(), task);
            return task;
        }

        @Override
        public Optional<ResumeTask> findById(String taskId) {
            return Optional.ofNullable(tasks.get(taskId));
        }
    }

    private static final class MemoryPatchStore implements PatchStore {
        private final Map<String, ResumePatch> patches = new LinkedHashMap<>();

        @Override
        public ResumePatch save(ResumePatch patch) {
            patches.put(patch.taskId() + ":" + patch.patchId(), patch);
            return patch;
        }

        @Override
        public Optional<ResumePatch> find(String taskId, String patchId) {
            return Optional.ofNullable(patches.get(taskId + ":" + patchId));
        }

        @Override
        public List<ResumePatch> findByTaskId(String taskId) {
            return patches.values().stream()
                    .filter(patch -> patch.taskId().equals(taskId))
                    .toList();
        }
    }

    private static final class MemoryResumeStore implements ResumeVersionStore {
        private final List<ObjectNode> resumes = new ArrayList<>();

        @Override
        public ObjectNode save(ObjectNode resume) {
            var copy = resume.deepCopy();
            resumes.add(copy);
            return copy.deepCopy();
        }

        @Override
        public Optional<ObjectNode> find(String resumeId, long version) {
            return resumes.stream()
                    .filter(resume -> resume.path("resumeId").asText().equals(resumeId))
                    .filter(resume -> resume.path("version").asLong() == version)
                    .findFirst().map(ObjectNode::deepCopy);
        }

        @Override
        public List<Long> versions(String resumeId) {
            return resumes.stream()
                    .filter(resume -> resume.path("resumeId").asText().equals(resumeId))
                    .map(resume -> resume.path("version").asLong())
                    .sorted().toList();
        }
    }

    private static final class EmptyEvidenceStore implements EvidenceArtifactStore {
        @Override public EvidenceArtifact save(EvidenceArtifact artifact) { return artifact; }
        @Override public Optional<EvidenceArtifact> findById(String artifactId) { return Optional.empty(); }
        @Override public List<EvidenceArtifact> findByTaskId(String taskId) { return List.of(); }
    }
}
