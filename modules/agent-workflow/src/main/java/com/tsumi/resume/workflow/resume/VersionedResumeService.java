package com.tsumi.resume.workflow.resume;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.regex.Pattern;

public final class VersionedResumeService {

    private static final Pattern RESUME_ID = Pattern.compile("^res_[A-Za-z0-9_-]+$");

    private final ResumeVersionStore store;

    public VersionedResumeService(ResumeVersionStore store) {
        this.store = store;
    }

    public ObjectNode register(ObjectNode resume) {
        var resumeId = resume.path("resumeId").asText();
        var version = resume.path("version").asLong();
        var schemaVersion = resume.path("schemaVersion").asInt();
        if (!RESUME_ID.matcher(resumeId).matches() || version < 1 || schemaVersion != 13) {
            throw new IllegalArgumentException("Invalid Resume AST envelope identity or version");
        }
        return store.save(resume.deepCopy()).deepCopy();
    }

    public ObjectNode get(String resumeId, long version) {
        return store.find(resumeId, version)
                .map(ObjectNode::deepCopy)
                .orElseThrow(() -> new ResumeVersionNotFoundException(resumeId, version));
    }

    public List<Long> versions(String resumeId) {
        return List.copyOf(store.versions(resumeId));
    }
}
