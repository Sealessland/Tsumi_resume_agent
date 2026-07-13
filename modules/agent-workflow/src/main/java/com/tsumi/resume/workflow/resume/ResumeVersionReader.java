package com.tsumi.resume.workflow.resume;

import com.fasterxml.jackson.databind.node.ObjectNode;

@FunctionalInterface
public interface ResumeVersionReader {

    ObjectNode get(String resumeId, long version);
}
