package com.tsumi.resume.server.review;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EditPatchRequest(
        @Min(1) long expectedBaseVersion,
        @NotNull @Size(max = 20_000) String after) {}
