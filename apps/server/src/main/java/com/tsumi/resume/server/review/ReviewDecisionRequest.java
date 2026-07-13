package com.tsumi.resume.server.review;

import com.tsumi.resume.domain.patch.ReviewStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewDecisionRequest(
        @Min(1) long expectedBaseVersion,
        @NotNull ReviewStatus decision) {}
