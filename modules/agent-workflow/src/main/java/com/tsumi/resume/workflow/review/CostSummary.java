package com.tsumi.resume.workflow.review;

import java.math.BigDecimal;

public record CostSummary(
        Long inputTokens,
        Long outputTokens,
        BigDecimal estimatedCost,
        String currency) {}
