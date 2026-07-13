package com.tsumi.resume.server.resume;

import java.util.List;

public record ResumeVersionsResponse(String resumeId, List<Long> versions) {

    public ResumeVersionsResponse {
        versions = List.copyOf(versions);
    }
}
