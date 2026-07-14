package com.tsumi.resume.ai.graph;

import com.tsumi.resume.domain.patch.PolicyDecision;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.workflow.review.ResumeReviewService;
import java.util.List;

public final class ReviewServiceProposalSink implements VerifiedProposalSink {

    private final ResumeReviewService reviews;

    public ReviewServiceProposalSink(ResumeReviewService reviews) {
        this.reviews = reviews;
    }

    @Override
    public void submit(String taskId, List<PatchProposal> proposals) {
        for (var proposal : proposals) {
            var result = reviews.submit(taskId, proposal);
            if (result.decision() != PolicyDecision.ALLOW) {
                throw new IllegalStateException("Verified proposal failed policy re-check during persistence");
            }
        }
    }
}
