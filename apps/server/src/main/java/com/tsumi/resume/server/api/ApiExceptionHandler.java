package com.tsumi.resume.server.api;

import com.tsumi.resume.task.TaskNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    ProblemDetail handleTaskNotFound(TaskNotFoundException exception) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Resume task not found");
        problem.setProperty("code", "TASK_NOT_FOUND");
        problem.setProperty("retryable", false);
        problem.setProperty("nextAction", "Check the task identifier.");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed.");
        problem.setTitle("Invalid request");
        problem.setProperty("code", "VALIDATION_ERROR");
        problem.setProperty("retryable", false);
        problem.setProperty("nextAction", "Correct the invalid request fields.");
        return problem;
    }
}
