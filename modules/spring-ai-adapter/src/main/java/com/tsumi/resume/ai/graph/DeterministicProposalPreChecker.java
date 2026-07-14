package com.tsumi.resume.ai.graph;

import com.tsumi.resume.domain.merge.PatchConflictException;
import com.tsumi.resume.domain.merge.ResumePathResolver;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.workflow.WorkflowInput;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class DeterministicProposalPreChecker implements ProposalPreChecker {

    private static final int MAX_PROPOSALS = 20;
    private final ResumePathResolver paths = new ResumePathResolver();

    @Override
    public PrecheckResult check(
            WorkflowInput input,
            ResumeModelView resume,
            List<PatchProposal> proposals) {
        var candidates = new ArrayList<PatchProposal>();
        var gaps = new ArrayList<CoverageGap>();
        var seenPaths = new HashSet<String>();
        if (proposals.size() > MAX_PROPOSALS) {
            return new PrecheckResult(List.of(), List.of(new CoverageGap(
                    "batch", "", List.of(), "proposal batch exceeds the server limit")));
        }
        for (var proposal : proposals) {
            var reason = validate(input, resume, proposal, seenPaths);
            if (reason == null) candidates.add(proposal);
            else gaps.add(new CoverageGap(proposal.patchId(), proposal.path(), List.of(), reason));
        }
        return new PrecheckResult(candidates, gaps);
    }

    private String validate(
            WorkflowInput input,
            ResumeModelView resume,
            PatchProposal proposal,
            HashSet<String> seenPaths) {
        if (!proposal.taskId().equals(input.taskId())
                || !proposal.resumeId().equals(input.resumeId())
                || proposal.baseVersion() != input.baseVersion()) {
            return "proposal target does not match the server workflow target";
        }
        if (!seenPaths.add(proposal.path())) return "duplicate proposal path";
        if (isProtectedIdentityPath(proposal.path())) return "proposal path targets protected identity data";
        if (proposal.evidenceRefs().isEmpty()) return "proposal has no approved Evidence reference";
        if (proposal.op() == PatchOperation.REMOVE && !proposal.after().isEmpty()) {
            return "remove proposal must have an empty after value";
        }
        try {
            var node = paths.resolve(resume.content(), proposal.path());
            if (!node.isTextual() || !node.textValue().equals(proposal.before())) {
                return "proposal before does not match the base resume";
            }
        } catch (PatchConflictException exception) {
            return "proposal path cannot be resolved in the base resume";
        }
        return null;
    }

    private boolean isProtectedIdentityPath(String path) {
        if (path == null || !path.startsWith("/")) return true;
        var tokens = path.substring(1).split("/");
        if (tokens.length == 0) return true;
        var leaf = tokens[tokens.length - 1].replace("~1", "/").replace("~0", "~");
        return leaf.equals("id") || (tokens.length == 1
                && (leaf.equals("resumeId") || leaf.equals("version") || leaf.equals("schemaVersion")));
    }
}
