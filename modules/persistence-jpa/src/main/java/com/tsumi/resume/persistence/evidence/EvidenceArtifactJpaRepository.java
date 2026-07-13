package com.tsumi.resume.persistence.evidence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceArtifactJpaRepository extends JpaRepository<EvidenceArtifactEntity, String> {
    List<EvidenceArtifactEntity> findByTaskIdOrderByCreatedAtAsc(String taskId);
}
