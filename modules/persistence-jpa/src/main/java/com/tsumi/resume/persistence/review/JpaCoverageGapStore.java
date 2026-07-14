package com.tsumi.resume.persistence.review;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsumi.resume.workflow.review.CoverageGap;
import com.tsumi.resume.workflow.review.CoverageGapStore;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaCoverageGapStore implements CoverageGapStore {
    private final CoverageGapJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaCoverageGapStore(CoverageGapJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void replace(String taskId, List<CoverageGap> gaps) {
        repository.deleteByTaskId(taskId);
        var entities = new ArrayList<CoverageGapEntity>();
        for (int index = 0; index < gaps.size(); index++) {
            entities.add(new CoverageGapEntity(taskId, index, write(gaps.get(index))));
        }
        repository.saveAllAndFlush(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoverageGap> findByTaskId(String taskId) {
        return repository.findByTaskIdOrderByOrdinalNoAsc(taskId).stream()
                .map(entity -> read(entity.gapJson()))
                .toList();
    }

    private String write(CoverageGap gap) {
        try {
            return objectMapper.writeValueAsString(gap);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Coverage gap cannot be serialized", exception);
        }
    }

    private CoverageGap read(String json) {
        try {
            return objectMapper.readValue(json, CoverageGap.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored coverage gap is invalid", exception);
        }
    }
}
