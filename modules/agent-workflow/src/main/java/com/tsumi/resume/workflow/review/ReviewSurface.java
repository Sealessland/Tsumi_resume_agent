package com.tsumi.resume.workflow.review;

import java.util.List;

public record ReviewSurface(
        String surfaceId,
        String taskId,
        long baseVersion,
        List<PatchReviewItem> patches,
        List<CoverageGap> gaps,
        List<TaskTimelineItem> timeline,
        List<AllowedAction> actions,
        CostSummary costSummary) {

    public ReviewSurface {
        patches = List.copyOf(patches);
        gaps = List.copyOf(gaps);
        timeline = List.copyOf(timeline);
        actions = List.copyOf(actions);
    }
}
