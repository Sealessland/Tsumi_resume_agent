package com.tsumi.resume.ai.graph;

import com.tsumi.resume.domain.policy.PatchProposal;
import java.util.List;

public record VerificationResult(List<PatchProposal> supported, List<CoverageGap> gaps) {
    public VerificationResult {
        supported = List.copyOf(supported);
        gaps = List.copyOf(gaps);
    }
}
