package com.tsumi.resume.server.review;

import jakarta.validation.constraints.Min;

public record MergePatchesRequest(@Min(1) long expectedBaseVersion) {}
