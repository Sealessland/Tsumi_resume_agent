package com.tsumi.resume.persistence.checkpoint;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowCheckpointJpaRepository extends JpaRepository<WorkflowCheckpointEntity, Long> {
    Optional<WorkflowCheckpointEntity> findFirstByTaskIdAndNodeNameOrderByIdDesc(String taskId, String nodeName);
    List<WorkflowCheckpointEntity> findByTaskIdOrderByIdAsc(String taskId);
}
