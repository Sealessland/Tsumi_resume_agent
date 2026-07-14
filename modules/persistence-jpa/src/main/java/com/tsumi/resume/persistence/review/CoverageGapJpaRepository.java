package com.tsumi.resume.persistence.review;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoverageGapJpaRepository extends JpaRepository<CoverageGapEntity, String> {
    List<CoverageGapEntity> findByTaskIdOrderByOrdinalNoAsc(String taskId);
    void deleteByTaskId(String taskId);
}
