package com.tsumi.resume.server.task;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank
        @Pattern(regexp = "^res_[A-Za-z0-9_-]+$")
        String resumeId,
        @Min(1)
        long baseVersion,
        @NotBlank
        @Size(max = 20_000)
        String jobDescription) {}
