package com.tsumi.resume.workflow.evidence;

import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import java.util.List;
import java.util.Optional;

public interface EvidenceArtifactStore {
    EvidenceArtifact save(EvidenceArtifact artifact);
    Optional<EvidenceArtifact> findById(String artifactId);
    List<EvidenceArtifact> findByTaskId(String taskId);
}
