package com.tsumi.resume.persistence.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsumi.resume.task.NewTaskEvent;
import com.tsumi.resume.task.TaskEvent;
import com.tsumi.resume.task.TaskEventStore;
import com.tsumi.resume.task.TaskStatus;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaTaskEventStore implements TaskEventStore {
    private final TaskEventJpaRepository repository;
    private final ObjectMapper objectMapper;
    public JpaTaskEventStore(TaskEventJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository; this.objectMapper = objectMapper;
    }
    @Override @Transactional
    public TaskEvent append(NewTaskEvent event) {
        try {
            var entity = repository.saveAndFlush(new TaskEventEntity(
                    event, objectMapper.writeValueAsString(event.data())));
            return toDomain(entity);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Event data cannot be serialized", exception);
        }
    }
    @Override @Transactional(readOnly = true)
    public List<TaskEvent> findAfter(String taskId, long afterExclusive, int limit) {
        if (afterExclusive < 0 || limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("Invalid event cursor or limit");
        }
        return repository.findByTaskIdAndEventIdGreaterThanOrderByEventIdAsc(
                        taskId, afterExclusive, PageRequest.of(0, limit)).stream()
                .map(this::toDomain).toList();
    }
    private TaskEvent toDomain(TaskEventEntity entity) {
        try {
            Map<String, String> data = objectMapper.readValue(entity.dataJson(), new TypeReference<>() {});
            return new TaskEvent(entity.eventId(), entity.taskId(), entity.eventType(),
                    TaskStatus.valueOf(entity.stage()), entity.attempt(), entity.traceId(),
                    entity.occurredAt(), data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored event data is invalid", exception);
        }
    }
}
