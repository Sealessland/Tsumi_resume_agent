package com.tsumi.resume.persistence.evidence;

import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaEvidenceArtifactStore implements EvidenceArtifactStore {
    private final EvidenceArtifactJpaRepository repository;
    public JpaEvidenceArtifactStore(EvidenceArtifactJpaRepository repository) { this.repository = repository; }
    @Override @Transactional
    public EvidenceArtifact save(EvidenceArtifact artifact) {
        if (repository.existsById(artifact.artifactId())) {
            throw new IllegalStateException("Evidence artifact is immutable: " + artifact.artifactId());
        }
        repository.saveAndFlush(new EvidenceArtifactEntity(artifact));
        return artifact;
    }
    @Override @Transactional(readOnly = true)
    public Optional<EvidenceArtifact> findById(String artifactId) {
        return repository.findById(artifactId).map(EvidenceArtifactEntity::toDomain);
    }
    @Override @Transactional(readOnly = true)
    public List<EvidenceArtifact> findByTaskId(String taskId) {
        return repository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(EvidenceArtifactEntity::toDomain).toList();
    }
}
