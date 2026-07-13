package com.tsumi.resume.domain.policy;

import com.tsumi.resume.domain.patch.PatchIntent;
import com.tsumi.resume.domain.patch.PatchOperation;
import java.util.List;

public record PatchProposal(
        String patchId,
        String taskId,
        String resumeId,
        long baseVersion,
        PatchOperation op,
        String path,
        String before,
        String after,
        PatchIntent intent,
        List<String> evidenceRefs,
        List<String> jdRefs,
        double confidence) {

    public PatchProposal {
        evidenceRefs = List.copyOf(evidenceRefs);
        jdRefs = List.copyOf(jdRefs);
    }
}
