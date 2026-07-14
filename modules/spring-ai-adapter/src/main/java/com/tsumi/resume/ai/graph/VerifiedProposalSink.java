package com.tsumi.resume.ai.graph;

import com.tsumi.resume.domain.policy.PatchProposal;
import java.util.List;

@FunctionalInterface
public interface VerifiedProposalSink {
    void submit(String taskId, List<PatchProposal> proposals);
}
