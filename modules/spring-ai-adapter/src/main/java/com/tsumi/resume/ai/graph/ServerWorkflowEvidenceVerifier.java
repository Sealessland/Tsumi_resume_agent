package com.tsumi.resume.ai.graph;

import com.tsumi.resume.domain.patch.PolicyDecision;
import com.tsumi.resume.domain.policy.PatchPolicy;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.task.TaskNotFoundException;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.workflow.WorkflowInput;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import com.tsumi.resume.workflow.evidence.EvidenceGuard;
import com.tsumi.resume.workflow.review.CoverageGap;
import com.tsumi.resume.workflow.evidence.EvidenceNotApprovedException;
import com.tsumi.resume.workflow.evidence.ProposalBaseMismatchException;
import java.util.ArrayList;
import java.util.List;

public final class ServerWorkflowEvidenceVerifier implements WorkflowEvidenceVerifier {

    private final TaskRepository tasks;
    private final EvidenceArtifactStore evidence;
    private final EvidenceGuard guard;
    private final PatchPolicy policy = new PatchPolicy();

    public ServerWorkflowEvidenceVerifier(
            TaskRepository tasks,
            EvidenceArtifactStore evidence,
            EvidenceGuard guard) {
        this.tasks = tasks;
        this.evidence = evidence;
        this.guard = guard;
    }

    @Override
    public VerificationResult verify(WorkflowInput input, List<PatchProposal> proposals) {
        var task = tasks.findById(input.taskId())
                .orElseThrow(() -> new TaskNotFoundException(input.taskId()));
        var artifacts = evidence.findByTaskId(input.taskId());
        var supported = new ArrayList<PatchProposal>();
        var gaps = new ArrayList<CoverageGap>();
        for (var proposal : proposals) {
            try {
                var assessment = guard.assess(task, proposal, artifacts);
                var evaluation = policy.evaluate(proposal, assessment);
                if (evaluation.decision() == PolicyDecision.ALLOW) {
                    supported.add(proposal);
                } else {
                    var unsupported = assessment.newAtomicClaims();
                    var reason = "Evidence Policy rejected: " + evaluation.violations();
                    gaps.add(new CoverageGap(proposal.patchId(), proposal.path(), unsupported, reason));
                }
            } catch (EvidenceNotApprovedException exception) {
                gaps.add(new CoverageGap(
                        proposal.patchId(), proposal.path(), List.of(), "Evidence is not approved or has expired"));
            } catch (ProposalBaseMismatchException exception) {
                gaps.add(new CoverageGap(
                        proposal.patchId(), proposal.path(), List.of(), "Proposal before does not match base resume"));
            } catch (IllegalArgumentException exception) {
                gaps.add(new CoverageGap(
                        proposal.patchId(), proposal.path(), List.of(), "Proposal target failed server validation"));
            }
        }
        return new VerificationResult(supported, gaps);
    }
}
