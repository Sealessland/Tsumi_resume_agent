package com.tsumi.resume.ai.graph;

import com.tsumi.resume.workflow.WorkflowInput;
import java.util.List;

public record RewriteRequest(
        WorkflowInput workflow,
        ResumeModelView resume,
        CapabilityMatrix capabilityMatrix,
        List<CoverageGap> gaps,
        boolean repair) {

    public RewriteRequest {
        gaps = List.copyOf(gaps);
    }
}
