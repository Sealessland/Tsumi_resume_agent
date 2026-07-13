package com.tsumi.resume.persistence.event;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskEventJpaRepository extends JpaRepository<TaskEventEntity, Long> {
    List<TaskEventEntity> findByTaskIdAndEventIdGreaterThanOrderByEventIdAsc(
            String taskId, long eventId, Pageable pageable);
}
