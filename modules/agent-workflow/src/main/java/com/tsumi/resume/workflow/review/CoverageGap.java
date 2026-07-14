package com.tsumi.resume.workflow.review;

import java.util.List;

public record CoverageGap(
        String patchId,
        String path,
        List<String> unsupportedClaims,
        String reason) {

    public CoverageGap {
        patchId = patchId == null || patchId.isBlank() ? "unassigned" : patchId;
        path = path == null ? "" : path;
        unsupportedClaims = List.copyOf(unsupportedClaims);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }
}
