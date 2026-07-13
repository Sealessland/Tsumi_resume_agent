package com.tsumi.resume.persistence.resume;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeVersionJpaRepository extends JpaRepository<ResumeVersionEntity, ResumeVersionId> {
    List<ResumeVersionEntity> findByIdResumeIdOrderByIdVersionAsc(String resumeId);
}
