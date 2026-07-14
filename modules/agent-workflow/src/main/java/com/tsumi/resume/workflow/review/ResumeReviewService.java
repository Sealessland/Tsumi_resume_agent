package com.tsumi.resume.workflow.review;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsumi.resume.domain.merge.ResumePatchEngine;
import com.tsumi.resume.domain.merge.VersionConflictException;
import com.tsumi.resume.domain.patch.ResumePatch;
import com.tsumi.resume.domain.patch.ReviewStatus;
import com.tsumi.resume.domain.policy.PatchPolicy;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.domain.policy.PolicyEvaluation;
import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.NewTaskEvent;
import com.tsumi.resume.task.TaskEvent;
import com.tsumi.resume.task.TaskEventStore;
import com.tsumi.resume.task.TaskNotFoundException;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.workflow.resume.VersionedResumeService;
import com.tsumi.resume.workflow.UnitOfWork;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import com.tsumi.resume.workflow.evidence.EvidenceGuard;
import java.time.Clock;
import java.util.List;
import java.util.Map;

public final class ResumeReviewService {

    private final TaskRepository taskRepository;
    private final PatchStore patchStore;
    private final VersionedResumeService resumeService;
    private final Clock clock;
    private final EvidenceArtifactStore evidenceStore;
    private final EvidenceGuard evidenceGuard;
    private final UnitOfWork unitOfWork;
    private final TaskEventStore taskEvents;
    private final PatchPolicy patchPolicy = new PatchPolicy();
    private final ResumePatchEngine patchEngine = new ResumePatchEngine();

    public ResumeReviewService(
            TaskRepository taskRepository,
            PatchStore patchStore,
            VersionedResumeService resumeService,
            Clock clock,
            EvidenceArtifactStore evidenceStore,
            EvidenceGuard evidenceGuard) {
        this(taskRepository, patchStore, resumeService, clock, evidenceStore, evidenceGuard,
                UnitOfWork.direct(), discardingEvents());
    }

    public ResumeReviewService(
            TaskRepository taskRepository,
            PatchStore patchStore,
            VersionedResumeService resumeService,
            Clock clock,
            EvidenceArtifactStore evidenceStore,
            EvidenceGuard evidenceGuard,
            TaskEventStore taskEvents) {
        this(taskRepository, patchStore, resumeService, clock, evidenceStore, evidenceGuard,
                UnitOfWork.direct(), taskEvents);
    }

    public ResumeReviewService(
            TaskRepository taskRepository,
            PatchStore patchStore,
            VersionedResumeService resumeService,
            Clock clock,
            EvidenceArtifactStore evidenceStore,
            EvidenceGuard evidenceGuard,
            UnitOfWork unitOfWork) {
        this(taskRepository, patchStore, resumeService, clock, evidenceStore, evidenceGuard,
                unitOfWork, discardingEvents());
    }

    public ResumeReviewService(
            TaskRepository taskRepository,
            PatchStore patchStore,
            VersionedResumeService resumeService,
            Clock clock,
            EvidenceArtifactStore evidenceStore,
            EvidenceGuard evidenceGuard,
            UnitOfWork unitOfWork,
            TaskEventStore taskEvents) {
        this.taskRepository = taskRepository;
        this.patchStore = patchStore;
        this.resumeService = resumeService;
        this.clock = clock;
        this.evidenceStore = evidenceStore;
        this.evidenceGuard = evidenceGuard;
        this.unitOfWork = unitOfWork;
        this.taskEvents = taskEvents;
    }

    public PolicyEvaluation submit(String taskId, PatchProposal proposal) {
        var task = task(taskId);
        requireProposalTarget(task, taskId, proposal);
        if (patchStore.find(taskId, proposal.patchId()).isPresent()) {
            throw new DuplicatePatchException(taskId, proposal.patchId());
        }

        var assessment = evidenceGuard.assess(
                task, proposal, evidenceStore.findByTaskId(taskId));
        var evaluation = patchPolicy.evaluate(proposal, assessment);
        evaluation.patch().ifPresent(patchStore::save);
        return evaluation;
    }

    public ResumePatch decide(
            String taskId,
            String patchId,
            long expectedBaseVersion,
            ReviewStatus decision) {
        var task = task(taskId);
        requireExpectedVersion(task, expectedBaseVersion);
        var patch = patchStore.find(taskId, patchId)
                .orElseThrow(() -> new PatchNotFoundException(taskId, patchId));
        if (patch.baseVersion() != expectedBaseVersion) {
            throw new VersionConflictException(patch.baseVersion(), expectedBaseVersion);
        }
        return patchStore.save(patch.reviewedAs(decision));
    }

    public List<ResumePatch> patches(String taskId) {
        task(taskId);
        return List.copyOf(patchStore.findByTaskId(taskId));
    }

    public ObjectNode merge(String taskId, long expectedBaseVersion) {
        return unitOfWork.execute(() -> mergeAtomically(taskId, expectedBaseVersion));
    }

    private ObjectNode mergeAtomically(String taskId, long expectedBaseVersion) {
        var task = task(taskId);
        requireExpectedVersion(task, expectedBaseVersion);
        var accepted = patchStore.findByTaskId(taskId).stream()
                .filter(patch -> patch.reviewStatus() == ReviewStatus.ACCEPTED)
                .toList();
        if (accepted.isEmpty()) {
            throw new NoAcceptedPatchesException(taskId);
        }

        for (var patch : accepted) {
            var proposal = new PatchProposal(
                    patch.patchId(), patch.taskId(), patch.resumeId(), patch.baseVersion(),
                    patch.op(), patch.path(), patch.before(), patch.after(), patch.intent(),
                    patch.evidenceRefs(), patch.jdRefs(), patch.confidence());
            var assessment = evidenceGuard.assess(
                    task, proposal, evidenceStore.findByTaskId(taskId));
            if (patchPolicy.evaluate(proposal, assessment).decision()
                    != com.tsumi.resume.domain.patch.PolicyDecision.ALLOW) {
                throw new ReviewConflictException("Accepted patch no longer passes Evidence Policy");
            }
        }

        var base = resumeService.get(task.resumeId(), expectedBaseVersion);
        var merged = patchEngine.applyAll(base, accepted);
        var saved = resumeService.register(merged);
        var approved = taskRepository.save(task.approve(clock.instant()));
        append(approved, "task.approved", Map.of(
                "resumeId", approved.resumeId(),
                "baseVersion", Long.toString(approved.baseVersion())));
        var completed = taskRepository.save(approved.complete(clock.instant()));
        append(completed, "task.completed", Map.of(
                "resumeId", completed.resumeId(),
                "version", saved.path("version").asText()));
        return saved;
    }

    private void append(ResumeTask task, String type, Map<String, String> data) {
        taskEvents.append(new NewTaskEvent(
                task.taskId(), type, task.status(), task.attempt(), task.traceId(),
                clock.instant(), data));
    }

    private static TaskEventStore discardingEvents() {
        return new TaskEventStore() {
            @Override
            public TaskEvent append(NewTaskEvent event) {
                return new TaskEvent(
                        1, event.taskId(), event.type(), event.stage(), event.attempt(),
                        event.traceId(), event.occurredAt(), event.data());
            }

            @Override
            public List<TaskEvent> findAfter(String taskId, long afterExclusive, int limit) {
                return List.of();
            }
        };
    }

    private ResumeTask task(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private void requireProposalTarget(
            ResumeTask task, String taskId, PatchProposal proposal) {
        if (!proposal.taskId().equals(taskId)) {
            throw new ReviewConflictException("Proposal taskId does not match request task");
        }
        if (!proposal.resumeId().equals(task.resumeId())) {
            throw new ReviewConflictException("Proposal resumeId does not match task resume");
        }
        if (proposal.baseVersion() != task.baseVersion()) {
            throw new VersionConflictException(task.baseVersion(), proposal.baseVersion());
        }
    }

    private void requireExpectedVersion(ResumeTask task, long expectedBaseVersion) {
        if (expectedBaseVersion != task.baseVersion()) {
            throw new VersionConflictException(task.baseVersion(), expectedBaseVersion);
        }
    }
}
