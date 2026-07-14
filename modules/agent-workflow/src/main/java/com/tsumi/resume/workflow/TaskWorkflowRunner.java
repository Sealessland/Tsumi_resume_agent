package com.tsumi.resume.workflow;

import com.tsumi.resume.task.NewTaskEvent;
import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskEventStore;
import com.tsumi.resume.task.TaskNotFoundException;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.task.TaskStatus;
import com.tsumi.resume.task.TaskVersionConflictException;
import com.tsumi.resume.task.WorkflowCheckpoint;
import com.tsumi.resume.task.WorkflowCheckpointStore;
import com.tsumi.resume.task.WorkflowNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;

public final class TaskWorkflowRunner {
    private final TaskRepository tasks;
    private final WorkflowRequestStore requests;
    private final ResumeAgentWorkflow workflow;
    private final TaskEventStore events;
    private final UnitOfWork unitOfWork;
    private final WorkflowCheckpointStore checkpoints;
    private final Clock clock;
    private final String leaseOwner;
    private final Duration leaseDuration;

    public TaskWorkflowRunner(
            TaskRepository tasks,
            WorkflowRequestStore requests,
            ResumeAgentWorkflow workflow,
            TaskEventStore events,
            UnitOfWork unitOfWork,
            WorkflowCheckpointStore checkpoints,
            Clock clock,
            String leaseOwner,
            Duration leaseDuration) {
        this.tasks = tasks;
        this.requests = requests;
        this.workflow = workflow;
        this.events = events;
        this.unitOfWork = unitOfWork;
        this.checkpoints = checkpoints;
        this.clock = clock;
        this.leaseOwner = leaseOwner;
        this.leaseDuration = leaseDuration;
    }

    public void run(String taskId) {
        var initial = task(taskId);
        if (!initial.status().isExecutionActive()) return;
        var request = requests.find(taskId)
                .orElseThrow(() -> new IllegalStateException("Workflow request is missing for " + taskId));
        try {
            var leased = mutate(initial.lease(leaseOwner, clock.instant().plus(leaseDuration), clock.instant()),
                    "task.lease.acquired", Map.of("leaseOwner", leaseOwner));
            var analyzing = leased.status() == TaskStatus.CREATED
                    ? mutate(leased.analyze(clock.instant()), "task.state.changed", stateChange(leased, TaskStatus.ANALYZING))
                    : leased;
            var result = workflow.execute(new WorkflowInput(
                    analyzing.taskId(), analyzing.resumeId(), analyzing.baseVersion(),
                    request.jobDescription(), analyzing.repairCount()),
                    observer(taskId));
            finishForReview(taskId, result);
        } catch (TaskVersionConflictException conflict) {
            if (task(taskId).status() != TaskStatus.CANCELLED) fail(taskId, "TASK_CONCURRENTLY_MODIFIED", true);
        } catch (WorkflowExecutionException exception) {
            fail(taskId, exception.code(), exception.retryable());
        } catch (RuntimeException exception) {
            fail(taskId, "WORKFLOW_FAILED", false);
        }
    }

    private void finishForReview(String taskId, WorkflowResult result) {
        var current = task(taskId);
        if (current.status() == TaskStatus.CANCELLED) return;
        if (current.status() == TaskStatus.ANALYZING) {
            var next = current.propose(clock.instant());
            current = mutate(next, "task.state.changed", stateChange(current, TaskStatus.PROPOSING));
        }
        if (current.status() == TaskStatus.PROPOSING) {
            var next = current.verify(clock.instant());
            current = mutate(next, "task.state.changed", stateChange(current, TaskStatus.VERIFYING));
        }
        if (current.status() == TaskStatus.VERIFYING) {
            var next = current.reviewReady(result.summary(), clock.instant());
            mutate(next, "task.review.ready", Map.of("reviewReady", "true"));
        }
    }

    private void fail(String taskId, String code, boolean retryable) {
        var current = task(taskId);
        if (!current.status().isExecutionActive()) return;
        try {
            mutate(current.fail(code, retryable, clock.instant()), "task.failed", Map.of(
                    "code", code, "retryable", Boolean.toString(retryable)));
        } catch (TaskVersionConflictException ignored) {
            // A concurrent cancellation or terminal transition wins over stale worker failure.
        }
    }

    private WorkflowObserver observer(String taskId) {
        return new WorkflowObserver() {
            @Override
            public void nodeStarted(com.tsumi.resume.task.WorkflowNode node) {
                var current = task(taskId);
                if (current.status() == TaskStatus.CANCELLED) {
                    throw new WorkflowExecutionException("TASK_CANCELLED", false, "Task was cancelled");
                }
                if (node == com.tsumi.resume.task.WorkflowNode.REWRITE_AGENT
                        && current.status() == TaskStatus.ANALYZING) {
                    var next = current.propose(clock.instant());
                    current = mutate(next, "task.state.changed", stateChange(current, TaskStatus.PROPOSING));
                } else if (node == com.tsumi.resume.task.WorkflowNode.DETERMINISTIC_PRECHECK
                        && current.status() == TaskStatus.PROPOSING) {
                    var next = current.verify(clock.instant());
                    current = mutate(next, "task.state.changed", stateChange(current, TaskStatus.VERIFYING));
                } else if (node == com.tsumi.resume.task.WorkflowNode.REPAIR_AGENT
                        && current.status() == TaskStatus.VERIFYING) {
                    var next = current.repair(clock.instant());
                    current = mutate(next, "task.repair.requested", stateChange(current, TaskStatus.PROPOSING));
                }
                checkpoints.save(WorkflowCheckpoint.started(
                        current.taskId(), node, current.attempt(), checkpointHash(current, node, "input"), clock.instant()));
                appendCurrent(current, "agent.node.started", Map.of("node", node.name()));
            }

            @Override
            public void nodeCompleted(com.tsumi.resume.task.WorkflowNode node, long durationMillis) {
                var current = task(taskId);
                var inputHash = checkpointHash(current, node, "input");
                checkpoints.save(WorkflowCheckpoint.completed(
                        current.taskId(), node, current.attempt(), inputHash,
                        checkpointHash(current, node, "output:" + durationMillis), clock.instant()));
                appendCurrent(current, "agent.node.completed", Map.of(
                        "node", node.name(), "durationMs", Long.toString(durationMillis)));
            }

            @Override
            public void nodeFailed(
                    com.tsumi.resume.task.WorkflowNode node,
                    String errorCode,
                    long durationMillis) {
                var current = task(taskId);
                checkpoints.save(WorkflowCheckpoint.failed(
                        current.taskId(), node, current.attempt(), checkpointHash(current, node, "input"),
                        errorCode, clock.instant()));
                appendCurrent(current, "agent.node.failed", Map.of(
                        "node", node.name(), "durationMs", Long.toString(durationMillis),
                        "errorCode", errorCode));
            }
        };
    }

    private void appendCurrent(ResumeTask task, String eventType, Map<String, String> data) {
        unitOfWork.execute(() -> {
            events.append(new NewTaskEvent(
                    task.taskId(), eventType, task.status(), task.attempt(),
                    task.traceId(), clock.instant(), data));
            return null;
        });
    }

    private ResumeTask mutate(ResumeTask next, String eventType, Map<String, String> data) {
        return unitOfWork.execute(() -> {
            var saved = tasks.save(next);
            events.append(new NewTaskEvent(
                    saved.taskId(), eventType, saved.status(), saved.attempt(),
                    saved.traceId(), clock.instant(), data));
            return saved;
        });
    }

    private Map<String, String> stateChange(ResumeTask previous, TaskStatus next) {
        return Map.of("from", previous.status().name(), "to", next.name());
    }

    private String checkpointHash(ResumeTask task, WorkflowNode node, String phase) {
        var value = task.taskId() + "\u0000" + task.attempt() + "\u0000" + node + "\u0000" + phase;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private ResumeTask task(String taskId) {
        return tasks.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
    }
}
