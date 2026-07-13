package com.tsumi.resume.persistence.review;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsumi.resume.domain.patch.ResumePatch;
import com.tsumi.resume.domain.patch.ReviewStatus;
import com.tsumi.resume.workflow.review.DuplicatePatchException;
import com.tsumi.resume.workflow.review.PatchStore;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaPatchStore implements PatchStore {
    private final PatchJpaRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JpaPatchStore(PatchJpaRepository repository, ObjectMapper objectMapper, Optional<Clock> clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock.orElseGet(Clock::systemUTC);
    }

    @Override @Transactional
    public ResumePatch save(ResumePatch patch) {
        var id = new PatchId(patch.taskId(), patch.patchId());
        var json = write(patch);
        var existing = repository.findById(id);
        if (existing.isEmpty()) {
            repository.saveAndFlush(new PatchEntity(patch, json, clock.instant()));
            return patch;
        }
        var entity = existing.orElseThrow();
        var current = read(entity.patchJson());
        if (current.reviewStatus() != ReviewStatus.PENDING
                || patch.reviewStatus() == ReviewStatus.PENDING) {
            throw new DuplicatePatchException(patch.taskId(), patch.patchId());
        }
        entity.update(patch, json, clock.instant());
        repository.saveAndFlush(entity);
        return patch;
    }

    @Override @Transactional(readOnly = true)
    public Optional<ResumePatch> find(String taskId, String patchId) {
        return repository.findById(new PatchId(taskId, patchId)).map(entity -> read(entity.patchJson()));
    }

    @Override @Transactional(readOnly = true)
    public List<ResumePatch> findByTaskId(String taskId) {
        return repository.findByIdTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(entity -> read(entity.patchJson())).toList();
    }

    private String write(ResumePatch patch) {
        try { return objectMapper.writeValueAsString(patch); }
        catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Patch cannot be serialized", exception);
        }
    }
    private ResumePatch read(String json) {
        try { return objectMapper.readValue(json, ResumePatch.class); }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored patch is invalid", exception);
        }
    }
}
