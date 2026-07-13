package com.tsumi.resume.infrastructure.evidence;

import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryEvidenceArtifactStore implements EvidenceArtifactStore {
    private final Map<String, EvidenceArtifact> artifacts = new LinkedHashMap<>();
    @Override public synchronized EvidenceArtifact save(EvidenceArtifact artifact) {
        if (artifacts.putIfAbsent(artifact.artifactId(), artifact) != null) {
            throw new IllegalStateException("Evidence artifact is immutable: " + artifact.artifactId());
        }
        return artifact;
    }
    @Override public synchronized Optional<EvidenceArtifact> findById(String artifactId) {
        return Optional.ofNullable(artifacts.get(artifactId));
    }
    @Override public synchronized List<EvidenceArtifact> findByTaskId(String taskId) {
        return artifacts.values().stream().filter(a -> a.taskId().equals(taskId)).toList();
    }
}
