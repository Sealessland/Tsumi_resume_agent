package com.tsumi.resume.ai.graph;

import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.workflow.WorkflowInput;
import java.util.List;

@FunctionalInterface
public interface WorkflowEvidenceVerifier {
    VerificationResult verify(WorkflowInput input, List<PatchProposal> proposals);
}
