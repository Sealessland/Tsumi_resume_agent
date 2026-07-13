package com.tsumi.resume.workflow.resume;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Optional;

public interface ResumeVersionStore {

    ObjectNode save(ObjectNode resume);

    Optional<ObjectNode> find(String resumeId, long version);

    List<Long> versions(String resumeId);
}
