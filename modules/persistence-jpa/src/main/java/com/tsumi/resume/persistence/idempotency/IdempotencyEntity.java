package com.tsumi.resume.persistence.idempotency;

import com.tsumi.resume.task.IdempotencyRecord;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "idempotency_record")
public class IdempotencyEntity {
    @EmbeddedId private IdempotencyId id;
    @Column(name = "request_hash", nullable = false, length = 96) private String requestHash;
    @Column(name = "response_status", nullable = false) private int responseStatus;
    @Column(name = "response_body", nullable = false, columnDefinition = "text") private String responseBody;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected IdempotencyEntity() {}
    public IdempotencyEntity(IdempotencyRecord record) {
        id = new IdempotencyId(record.scope(), record.key()); requestHash = record.requestHash();
        responseStatus = record.responseStatus(); responseBody = record.responseBody(); createdAt = record.createdAt();
    }
    public IdempotencyRecord toDomain() {
        return new IdempotencyRecord(id.scope(), id.key(), requestHash, responseStatus, responseBody, createdAt);
    }
}
