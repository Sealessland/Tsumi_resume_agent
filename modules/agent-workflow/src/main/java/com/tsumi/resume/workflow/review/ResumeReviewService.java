package com.tsumi.resume.workflow.review;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsumi.resume.domain.merge.ResumePatchEngine;
import com.tsumi.resume.domain.merge.VersionConflictException;
import com.tsumi.resume.domain.patch.ResumePatch;
import com.tsumi.resume.domain.patch.ReviewStatus;
import com.tsumi.resume.domain.policy.PatchAssessment;
import com.tsumi.resume.domain.policy.PatchPolicy;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.domain.policy.PolicyEvaluation;
import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskNotFoundException;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.workflow.resume.VersionedResumeService;
import java.time.Clock;

public final class ResumeReviewService {

    private final TaskRepository taskRepository;
    private final PatchStore patchStore;
    private final VersionedResumeService resumeService;
    private final Clock clock;
    private final PatchPolicy patchPolicy = new PatchPolicy();
    private final ResumePatchEngine patchEngine = new ResumePatchEngine();

    public ResumeReviewService(
            TaskRepository taskRepository,
            PatchStore patchStore,
            VersionedResumeService resumeService,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.patchStore = patchStore;
        this.resumeService = resumeService;
        this.clock = clock;
    }

    public PolicyEvaluation submit(
            String taskId, PatchProposal proposal, PatchAssessment assessment) {
        var task = task(taskId);
        requireProposalTarget(task, taskId, proposal);
        if (patchStore.find(taskId, proposal.patchId()).isPresent()) {
            throw new DuplicatePatchException(taskId, proposal.patchId());
        }

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

    public ObjectNode merge(String taskId, long expectedBaseVersion) {
        var task = task(taskId);
        requireExpectedVersion(task, expectedBaseVersion);
        var accepted = patchStore.findByTaskId(taskId).stream()
                .filter(patch -> patch.reviewStatus() == ReviewStatus.ACCEPTED)
                .toList();
        if (accepted.isEmpty()) {
            throw new NoAcceptedPatchesException(taskId);
        }

        var base = resumeService.get(task.resumeId(), expectedBaseVersion);
        var merged = patchEngine.applyAll(base, accepted);
        var saved = resumeService.register(merged);
        taskRepository.save(task.complete(clock.instant()));
        return saved;
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
