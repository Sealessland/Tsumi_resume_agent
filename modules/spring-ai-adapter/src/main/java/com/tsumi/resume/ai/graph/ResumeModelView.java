package com.tsumi.resume.ai.graph;

import com.fasterxml.jackson.databind.node.ObjectNode;

public record ResumeModelView(String resumeId, long version, ObjectNode content) {
    public ResumeModelView {
        if (resumeId == null || resumeId.isBlank()) throw new IllegalArgumentException("resumeId must not be blank");
        if (version < 1) throw new IllegalArgumentException("version must be positive");
        content = content.deepCopy();
    }

    @Override
    public ObjectNode content() {
        return content.deepCopy();
    }
}
