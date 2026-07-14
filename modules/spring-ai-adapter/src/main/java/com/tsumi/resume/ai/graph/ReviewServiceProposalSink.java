package com.tsumi.resume.ai.graph;

import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.workflow.review.ResumeReviewService;
import com.tsumi.resume.workflow.review.CoverageGap;
import java.util.List;

public final class ReviewServiceProposalSink implements VerifiedProposalSink {

    private final ResumeReviewService reviews;

    public ReviewServiceProposalSink(ResumeReviewService reviews) {
        this.reviews = reviews;
    }

    @Override
    public void submit(String taskId, List<PatchProposal> proposals, List<CoverageGap> gaps) {
        reviews.submitWorkflowResult(taskId, proposals, gaps);
    }
}
