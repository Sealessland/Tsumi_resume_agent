package com.tsumi.resume.server.api;

import com.tsumi.resume.domain.merge.PatchConflictException;
import com.tsumi.resume.domain.merge.VersionConflictException;
import com.tsumi.resume.task.TaskNotFoundException;
import com.tsumi.resume.workflow.resume.DuplicateResumeVersionException;
import com.tsumi.resume.workflow.resume.ResumeVersionNotFoundException;
import com.tsumi.resume.workflow.review.DuplicatePatchException;
import com.tsumi.resume.workflow.review.NoAcceptedPatchesException;
import com.tsumi.resume.workflow.review.PatchNotFoundException;
import com.tsumi.resume.workflow.review.ReviewConflictException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

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
