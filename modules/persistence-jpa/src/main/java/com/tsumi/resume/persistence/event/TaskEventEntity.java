package com.tsumi.resume.persistence.event;

import com.tsumi.resume.task.NewTaskEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "task_event")
public class TaskEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id") private Long eventId;
    @Column(name = "task_id", nullable = false, length = 80) private String taskId;
    @Column(name = "event_type", nullable = false, length = 100) private String eventType;
    @Column(name = "stage", nullable = false, length = 32) private String stage;
    @Column(name = "attempt", nullable = false) private int attempt;
    @Column(name = "trace_id", nullable = false, length = 80) private String traceId;
    @Column(name = "data_json", nullable = false, columnDefinition = "text") private String dataJson;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;

    protected TaskEventEntity() {}
    public TaskEventEntity(NewTaskEvent event, String dataJson) {
        taskId = event.taskId(); eventType = event.type(); stage = event.stage().name();
        attempt = event.attempt(); traceId = event.traceId(); this.dataJson = dataJson;
        occurredAt = event.occurredAt();
    }
    public Long eventId() { return eventId; }
    public String taskId() { return taskId; }
    public String eventType() { return eventType; }
    public String stage() { return stage; }
    public int attempt() { return attempt; }
    public String traceId() { return traceId; }
    public String dataJson() { return dataJson; }
    public Instant occurredAt() { return occurredAt; }
}
