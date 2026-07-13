package com.tsumi.resume.persistence.task;

import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.task.TaskVersionConflictException;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public class JpaTaskRepositoryAdapter implements TaskRepository {

    private final TaskJpaRepository repository;

    public JpaTaskRepositoryAdapter(TaskJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ResumeTask save(ResumeTask task) {
        var existing = repository.findById(task.taskId());
        if (existing.isEmpty()) {
            if (task.revision() != 0) {
                throw new TaskVersionConflictException(task.taskId(), 0, task.revision());
            }
            return repository.saveAndFlush(new TaskEntity(task)).toDomain();
        }

        var entity = existing.orElseThrow();
        if (task.revision() == entity.domainRevision() && task.equals(entity.toDomain())) {
            return task;
        }
        var expectedRevision = entity.domainRevision() + 1;
        if (task.revision() < expectedRevision) {
            throw new TaskVersionConflictException(
                    task.taskId(), expectedRevision, task.revision());
        }
        entity.update(task);
        return repository.saveAndFlush(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResumeTask> findById(String taskId) {
        return repository.findById(taskId).map(TaskEntity::toDomain);
    }
}
