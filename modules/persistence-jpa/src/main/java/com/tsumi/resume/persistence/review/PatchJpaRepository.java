package com.tsumi.resume.persistence.review;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatchJpaRepository extends JpaRepository<PatchEntity, PatchId> {
    List<PatchEntity> findByIdTaskIdOrderByCreatedAtAsc(String taskId);
}
