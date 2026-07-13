package com.tsumi.resume.persistence.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsumi.resume.workflow.resume.DuplicateResumeVersionException;
import com.tsumi.resume.workflow.resume.ResumeVersionStore;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaResumeVersionStore implements ResumeVersionStore {

    private final ResumeVersionJpaRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JpaResumeVersionStore(
            ResumeVersionJpaRepository repository,
            ObjectMapper objectMapper,
            Optional<Clock> clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock.orElseGet(Clock::systemUTC);
    }

    @Override
    @Transactional
    public ObjectNode save(ObjectNode resume) {
        var id = new ResumeVersionId(resume.path("resumeId").asText(), resume.path("version").asLong());
        if (repository.existsById(id)) {
            throw new DuplicateResumeVersionException(id.resumeId(), id.version());
        }
        try {
            repository.saveAndFlush(new ResumeVersionEntity(
                    id, resume.path("schemaVersion").asInt(),
                    objectMapper.writeValueAsString(resume), clock.instant()));
            return resume.deepCopy();
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Resume AST cannot be serialized", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ObjectNode> find(String resumeId, long version) {
        return repository.findById(new ResumeVersionId(resumeId, version))
                .map(entity -> read(entity.documentJson()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> versions(String resumeId) {
        return repository.findByIdResumeIdOrderByIdVersionAsc(resumeId).stream()
                .map(entity -> entity.id().version()).toList();
    }

    private ObjectNode read(String json) {
        try { return (ObjectNode) objectMapper.readTree(json); }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored Resume AST is invalid", exception);
        }
    }
}
