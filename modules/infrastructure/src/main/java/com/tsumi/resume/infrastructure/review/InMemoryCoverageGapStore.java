package com.tsumi.resume.infrastructure.review;

import com.tsumi.resume.workflow.review.CoverageGap;
import com.tsumi.resume.workflow.review.CoverageGapStore;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryCoverageGapStore implements CoverageGapStore {
    private final ConcurrentMap<String, List<CoverageGap>> gaps = new ConcurrentHashMap<>();

    @Override
    public void replace(String taskId, List<CoverageGap> values) {
        if (values.isEmpty()) gaps.remove(taskId);
        else gaps.put(taskId, List.copyOf(values));
    }

    @Override
    public List<CoverageGap> findByTaskId(String taskId) {
        return gaps.getOrDefault(taskId, List.of());
    }
}
