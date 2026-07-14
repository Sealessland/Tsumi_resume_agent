package com.tsumi.resume.ai.graph;

import com.tsumi.resume.domain.policy.PatchProposal;
import java.util.List;

public record PrecheckResult(List<PatchProposal> candidates, List<CoverageGap> gaps) {
    public PrecheckResult {
        candidates = List.copyOf(candidates);
        gaps = List.copyOf(gaps);
    }

    public static PrecheckResult ready(List<PatchProposal> candidates) {
        return new PrecheckResult(candidates, List.of());
    }
}
