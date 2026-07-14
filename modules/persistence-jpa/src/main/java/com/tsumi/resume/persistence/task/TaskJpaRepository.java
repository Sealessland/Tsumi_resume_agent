package com.tsumi.resume.persistence.task;

import com.tsumi.resume.task.TaskStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, String> {
    @Query("""
            select task from TaskEntity task
            where task.status in :statuses
              and (task.leaseUntil is null or task.leaseUntil < :now)
            order by task.updatedAt asc
            """)
    List<TaskEntity> findRecoverable(
            @Param("statuses") List<TaskStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable);
}
