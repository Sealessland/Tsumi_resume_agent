package com.tsumi.resume.server.config;

import com.tsumi.resume.infrastructure.contract.JsonContractValidator;
import com.tsumi.resume.infrastructure.resume.InMemoryResumeVersionStore;
import com.tsumi.resume.infrastructure.review.InMemoryPatchStore;
import com.tsumi.resume.infrastructure.review.InMemoryCoverageGapStore;
import com.tsumi.resume.infrastructure.task.InMemoryTaskRepository;
import com.tsumi.resume.infrastructure.task.InMemoryTaskEventStore;
import com.tsumi.resume.infrastructure.task.InMemoryIdempotencyStore;
import com.tsumi.resume.infrastructure.workflow.LocalDeterministicWorkflow;
import com.tsumi.resume.infrastructure.workflow.InMemoryWorkflowRequestStore;
import com.tsumi.resume.infrastructure.workflow.InMemoryWorkflowCheckpointStore;
import com.tsumi.resume.infrastructure.evidence.InMemoryEvidenceArtifactStore;
import com.tsumi.resume.domain.evidence.ClaimAssessment;
import com.tsumi.resume.domain.evidence.ClaimVerdict;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.task.TaskEventStore;
import com.tsumi.resume.task.IdempotencyStore;
import com.tsumi.resume.task.WorkflowCheckpointStore;
import com.tsumi.resume.workflow.ResumeAgentWorkflow;
import com.tsumi.resume.workflow.TaskOrchestrator;
import com.tsumi.resume.workflow.UnitOfWork;
import com.tsumi.resume.workflow.WorkflowRequestStore;
import com.tsumi.resume.workflow.resume.ResumeVersionStore;
import com.tsumi.resume.workflow.resume.ResumeVersionReader;
import com.tsumi.resume.workflow.resume.VersionedResumeService;
import com.tsumi.resume.workflow.review.PatchStore;
import com.tsumi.resume.workflow.review.ResumeReviewService;
import com.tsumi.resume.workflow.review.CoverageGapStore;
import com.tsumi.resume.workflow.review.ReviewSurfaceService;
import com.tsumi.resume.workflow.evidence.ClaimSupportEvaluator;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import com.tsumi.resume.workflow.evidence.EvidenceGuard;
import com.tsumi.resume.workflow.evidence.ServerEvidenceGuard;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local-demo", "test"})
public class LocalRuntimeConfiguration {

    @Bean
    TaskRepository taskRepository() {
        return new InMemoryTaskRepository();
    }

    @Bean
    TaskEventStore taskEventStore() {
        return new InMemoryTaskEventStore();
    }

    @Bean
    IdempotencyStore idempotencyStore() {
        return new InMemoryIdempotencyStore();
    }

    @Bean
    WorkflowRequestStore workflowRequestStore() {
        return new InMemoryWorkflowRequestStore();
    }

    @Bean
    WorkflowCheckpointStore workflowCheckpointStore() {
        return new InMemoryWorkflowCheckpointStore();
    }

    @Bean
    UnitOfWork unitOfWork() {
        return UnitOfWork.direct();
    }

    @Bean
    ResumeAgentWorkflow resumeAgentWorkflow() {
        return new LocalDeterministicWorkflow();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ResumeVersionStore resumeVersionStore() {
        return new InMemoryResumeVersionStore();
    }

    @Bean
    VersionedResumeService versionedResumeService(ResumeVersionStore store) {
        return new VersionedResumeService(store);
    }

    @Bean
    PatchStore patchStore() {
        return new InMemoryPatchStore();
    }

    @Bean
    CoverageGapStore coverageGapStore() {
        return new InMemoryCoverageGapStore();
    }

    @Bean
    EvidenceArtifactStore evidenceArtifactStore() {
        return new InMemoryEvidenceArtifactStore();
    }

    @Bean
    ClaimSupportEvaluator claimSupportEvaluator() {
        return (before, after, evidence) -> List.of(new ClaimAssessment(
                after,
                evidence.isEmpty() ? ClaimVerdict.AMBIGUOUS : ClaimVerdict.SUPPORTED,
                evidence.stream().map(artifact -> artifact.artifactId()).toList(),
                evidence.isEmpty()
                        ? "No approved Evidence is available"
                        : "Deterministic local recording accepted the evidence-backed paraphrase"));
    }

    @Bean
    EvidenceGuard evidenceGuard(
            ResumeVersionReader resumes,
            ClaimSupportEvaluator evaluator,
            Clock clock) {
        return new ServerEvidenceGuard(resumes, evaluator, clock);
    }

    @Bean
    ResumeReviewService resumeReviewService(
            TaskRepository taskRepository,
            PatchStore patchStore,
            VersionedResumeService resumeService,
            Clock clock,
            EvidenceArtifactStore evidenceStore,
            EvidenceGuard evidenceGuard,
            UnitOfWork unitOfWork,
            TaskEventStore taskEvents,
            CoverageGapStore coverageGaps) {
        return new ResumeReviewService(
                taskRepository, patchStore, resumeService, clock, evidenceStore, evidenceGuard,
                unitOfWork, taskEvents, coverageGaps);
    }

    @Bean
    ReviewSurfaceService reviewSurfaceService(
            TaskRepository tasks,
            PatchStore patches,
            EvidenceArtifactStore evidence,
            TaskEventStore events,
            CoverageGapStore gaps) {
        return new ReviewSurfaceService(tasks, patches, evidence, events, gaps);
    }

    @Bean("resumeContractValidator")
    JsonContractValidator resumeContractValidator() throws IOException {
        return packagedContract("contracts/resume.schema.json");
    }

    @Bean("patchProposalContractValidator")
    JsonContractValidator patchProposalContractValidator() throws IOException {
        return packagedContract("contracts/resume-patch-proposal.schema.json");
    }

    @Bean
    TaskOrchestrator taskOrchestrator(
            TaskRepository taskRepository,
            ResumeVersionReader resumeVersionReader,
            ResumeAgentWorkflow workflow,
            Clock clock) {
        return new TaskOrchestrator(
                taskRepository,
                resumeVersionReader,
                workflow,
                clock,
                () -> "task_" + UUID.randomUUID().toString().replace("-", ""));
    }

    private JsonContractValidator packagedContract(String path) throws IOException {
        return new JsonContractValidator(new ClassPathResource(path).getInputStream());
    }
}
