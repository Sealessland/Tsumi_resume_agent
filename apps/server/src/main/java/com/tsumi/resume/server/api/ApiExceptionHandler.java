package com.tsumi.resume.server.api;

import com.tsumi.resume.domain.merge.PatchConflictException;
import com.tsumi.resume.domain.merge.VersionConflictException;
import com.tsumi.resume.task.TaskNotFoundException;
import com.tsumi.resume.task.TaskCancelledException;
import com.tsumi.resume.task.TaskNotRetryableException;
import com.tsumi.resume.task.IdempotencyConflictException;
import com.tsumi.resume.server.task.SseCursorInvalidException;
import com.tsumi.resume.server.task.SseConnectionRejectedException;
import com.tsumi.resume.server.review.A2uiPayloadLimitException;
import com.tsumi.resume.workflow.resume.DuplicateResumeVersionException;
import com.tsumi.resume.workflow.resume.ResumeVersionNotFoundException;
import com.tsumi.resume.workflow.review.DuplicatePatchException;
import com.tsumi.resume.workflow.review.NoAcceptedPatchesException;
import com.tsumi.resume.workflow.review.PatchNotFoundException;
import com.tsumi.resume.workflow.review.ReviewConflictException;
import com.tsumi.resume.workflow.WorkflowExecutionException;
import com.tsumi.resume.workflow.evidence.EvidenceNotApprovedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(TaskNotRetryableException.class)
    ProblemDetail handleTaskNotRetryable(TaskNotRetryableException exception) {
        return problem(HttpStatus.CONFLICT, "Task is not retryable", exception.getMessage(),
                "TASK_NOT_RETRYABLE", "Inspect the task failure and create a new task if needed.");
    }

    @ExceptionHandler(TaskCancelledException.class)
    ProblemDetail handleTaskCancelled(TaskCancelledException exception) {
        return problem(HttpStatus.CONFLICT, "Task is cancelled", exception.getMessage(),
                "TASK_CANCELLED", "Create a new task; cancelled tasks cannot resume.");
    }

    @ExceptionHandler(EvidenceNotApprovedException.class)
    ProblemDetail handleEvidenceNotApproved(EvidenceNotApprovedException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Evidence is not approved", exception.getMessage(),
                "EVIDENCE_NOT_APPROVED", "Approve valid task-scoped Evidence before retrying.");
    }

    @ExceptionHandler(WorkflowExecutionException.class)
    ProblemDetail handleWorkflowExecution(WorkflowExecutionException exception) {
        var status = exception.code().equals("WORKFLOW_TIMEOUT")
                ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.UNPROCESSABLE_ENTITY;
        var problem = problem(status, "Agent workflow failed", exception.getMessage(),
                exception.code(), "Retry only when the response marks the failure retryable.");
        problem.setProperty("retryable", exception.retryable());
        return problem;
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail handleIdempotencyConflict(IdempotencyConflictException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "Idempotency conflict",
                exception.getMessage(),
                "IDEMPOTENCY_CONFLICT",
                "Use the original request payload or a new Idempotency-Key.");
    }

    @ExceptionHandler(SseCursorInvalidException.class)
    ProblemDetail handleSseCursor(SseCursorInvalidException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid SSE cursor",
                exception.getMessage(),
                "SSE_CURSOR_INVALID",
                "Reconnect without Last-Event-ID to replay this task stream from the beginning.");
    }

    @ExceptionHandler(SseConnectionRejectedException.class)
    ProblemDetail handleSseConnection(SseConnectionRejectedException exception) {
        return problem(
                exception.capacity() ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.CONFLICT,
                "SSE connection rejected",
                exception.getMessage(),
                "SSE_CONNECTION_REJECTED",
                "Close the existing stream or reconnect after capacity is available.");
    }

    @ExceptionHandler(A2uiPayloadLimitException.class)
    ProblemDetail handleA2uiLimit(A2uiPayloadLimitException exception) {
        return problem(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "A2UI surface limit exceeded",
                exception.getMessage(),
                "A2UI_PAYLOAD_LIMIT",
                "Reduce the number or size of review items before rendering.");
    }

    @ExceptionHandler(TaskNotFoundException.class)
    ProblemDetail handleTaskNotFound(TaskNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Resume task not found",
                exception.getMessage(),
                "TASK_NOT_FOUND",
                "Check the task identifier.");
    }

    @ExceptionHandler(ResumeVersionNotFoundException.class)
    ProblemDetail handleResumeNotFound(ResumeVersionNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Resume version not found",
                exception.getMessage(),
                "RESUME_VERSION_NOT_FOUND",
                "Import the requested resume version first.");
    }

    @ExceptionHandler(PatchNotFoundException.class)
    ProblemDetail handlePatchNotFound(PatchNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Patch not found",
                exception.getMessage(),
                "PATCH_NOT_FOUND",
                "Refresh the task patches before reviewing.");
    }

    @ExceptionHandler(ContractRejectedException.class)
    ProblemDetail handleContractRejected(ContractRejectedException exception) {
        var problem = problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Contract rejected",
                exception.getMessage(),
                "CONTRACT_REJECTED",
                "Correct the payload to match the published JSON Schema.");
        problem.setProperty("contract", exception.contract());
        problem.setProperty("violations", exception.violations());
        return problem;
    }

    @ExceptionHandler(PolicyRejectedException.class)
    ProblemDetail handlePolicyRejected(PolicyRejectedException exception) {
        var problem = problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Evidence policy rejected the patch",
                exception.getMessage(),
                "POLICY_REJECTED",
                "Use only claims fully supported by cited resume evidence.");
        problem.setProperty("violations", exception.violations());
        return problem;
    }

    @ExceptionHandler({DuplicateResumeVersionException.class, DuplicatePatchException.class})
    ProblemDetail handleDuplicate(RuntimeException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "Immutable resource already exists",
                exception.getMessage(),
                "DUPLICATE_RESOURCE",
                "Use a new identifier or immutable version number.");
    }

    @ExceptionHandler(VersionConflictException.class)
    ProblemDetail handleVersionConflict(VersionConflictException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "Resume version conflict",
                exception.getMessage(),
                "VERSION_CONFLICT",
                "Reload the latest task and resume version before retrying.");
    }

    @ExceptionHandler({
        ReviewConflictException.class,
        PatchConflictException.class,
        NoAcceptedPatchesException.class,
        IllegalStateException.class
    })
    ProblemDetail handleWorkflowConflict(RuntimeException exception) {
        return problem(
                HttpStatus.CONFLICT,
                "Review workflow conflict",
                exception.getMessage(),
                "REVIEW_CONFLICT",
                "Refresh task state and review decisions before retrying.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "Request validation failed.",
                "VALIDATION_ERROR",
                "Correct the invalid request fields.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadable(HttpMessageNotReadableException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request",
                "Request body is missing or cannot be decoded.",
                "MALFORMED_JSON",
                "Send valid JSON using the documented enum values and field types.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                exception.getMessage(),
                "INVALID_ARGUMENT",
                "Correct the request and retry.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        var traceId = "trace_" + UUID.randomUUID().toString().replace("-", "");
        LOG.error("Unexpected API failure traceId={}", traceId, exception);
        var problem = problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "The request could not be completed.",
                "INTERNAL_ERROR",
                "Retry later and provide the traceId if the problem persists.");
        problem.setProperty("traceId", traceId);
        return problem;
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail,
            String code,
            String nextAction) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("code", code);
        problem.setProperty("retryable", false);
        problem.setProperty("nextAction", nextAction);
        return problem;
    }
}
