package com.tsumi.resume.workflow.review;

import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import com.tsumi.resume.domain.patch.PolicyDecision;
import com.tsumi.resume.domain.patch.ReviewStatus;
import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskEvent;
import com.tsumi.resume.task.TaskEventStore;
import com.tsumi.resume.task.TaskNotFoundException;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.task.TaskStatus;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ReviewSurfaceService {
    private final TaskRepository tasks;
    private final PatchStore patches;
    private final EvidenceArtifactStore evidence;
    private final TaskEventStore events;
    private final CoverageGapStore gaps;

    public ReviewSurfaceService(
            TaskRepository tasks,
            PatchStore patches,
            EvidenceArtifactStore evidence,
            TaskEventStore events,
            CoverageGapStore gaps) {
        this.tasks = tasks;
        this.patches = patches;
        this.evidence = evidence;
        this.events = events;
        this.gaps = gaps;
    }

    public ReviewSurface get(String taskId) {
        var task = tasks.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        var artifacts = evidence.findByTaskId(taskId).stream()
                .collect(Collectors.toMap(EvidenceArtifact::artifactId, Function.identity()));
        var patchItems = patches.findByTaskId(taskId).stream()
                .map(patch -> new PatchReviewItem(
                        patch.patchId(), patch.path(), patch.op(), patch.before(), patch.after(),
                        patch.evidenceRefs().stream()
                                .map(artifacts::get)
                                .filter(java.util.Objects::nonNull)
                                .map(this::evidenceView)
                                .toList(),
                        patch.claimAssessments(), patch.policyDecision(), patch.riskFlags(),
                        patch.reviewStatus(), patchActions(task, patch.policyDecision(), patch.reviewStatus()),
                        task.baseVersion()))
                .toList();
        var timeline = events.findAfter(taskId, 0, 1000).stream()
                .map(this::timelineItem)
                .toList();
        return new ReviewSurface(
                "review_" + task.taskId() + "_v" + task.baseVersion(),
                task.taskId(), task.baseVersion(), patchItems,
                gaps.findByTaskId(taskId), timeline, taskActions(task, patchItems),
                costSummary(timeline));
    }

    private EvidenceView evidenceView(EvidenceArtifact artifact) {
        return new EvidenceView(
                artifact.artifactId(), artifact.sourceType(), artifact.sourceRef(),
                artifact.excerpt(), artifact.contentHash(), artifact.approvalStatus(),
                artifact.expiresAt());
    }

    private TaskTimelineItem timelineItem(TaskEvent event) {
        return new TaskTimelineItem(
                event.eventId(), event.type(), event.stage(), event.attempt(),
                event.occurredAt(), event.data());
    }

    private List<AllowedAction> patchActions(
            ResumeTask task,
            PolicyDecision policy,
            ReviewStatus reviewStatus) {
        if (task.status() != TaskStatus.REVIEW_READY
                || policy != PolicyDecision.ALLOW
                || reviewStatus != ReviewStatus.PENDING) {
            return List.of();
        }
        return List.of(AllowedAction.ACCEPT, AllowedAction.REJECT, AllowedAction.EDIT);
    }

    private List<AllowedAction> taskActions(
            ResumeTask task,
            List<PatchReviewItem> patchItems) {
        var actions = new ArrayList<AllowedAction>();
        if (task.status().isExecutionActive() || task.status() == TaskStatus.REVIEW_READY) {
            actions.add(AllowedAction.CANCEL);
        }
        if (task.status() == TaskStatus.FAILED && task.failureRetryable()) {
            actions.add(AllowedAction.RETRY);
        }
        if (task.status() == TaskStatus.REVIEW_READY
                && patchItems.stream().anyMatch(item -> item.reviewStatus() == ReviewStatus.ACCEPTED)) {
            actions.add(AllowedAction.MERGE);
        }
        return List.copyOf(actions);
    }

    private CostSummary costSummary(List<TaskTimelineItem> timeline) {
        return new CostSummary(
                sumIfPresent(timeline, "inputTokens"),
                sumIfPresent(timeline, "outputTokens"),
                null,
                null);
    }

    private Long sumIfPresent(List<TaskTimelineItem> timeline, String key) {
        long total = 0;
        boolean present = false;
        for (var item : timeline) {
            var raw = item.data().get(key);
            if (raw == null) continue;
            try {
                total = Math.addExact(total, Long.parseLong(raw));
                present = true;
            } catch (ArithmeticException | NumberFormatException ignored) {
                // Untrusted or malformed metadata is omitted instead of being estimated.
            }
        }
        return present ? total : null;
    }
}
