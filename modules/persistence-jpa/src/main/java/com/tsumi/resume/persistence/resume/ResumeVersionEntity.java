package com.tsumi.resume.persistence.resume;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "resume_version")
public class ResumeVersionEntity {

    @EmbeddedId private ResumeVersionId id;
    @Column(name = "schema_version", nullable = false) private int schemaVersion;
    @Column(name = "document_json", nullable = false, columnDefinition = "text") private String documentJson;
    @Column(name = "actor", nullable = false, length = 80) private String actor;
    @Column(name = "source_task_id", length = 80) private String sourceTaskId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected ResumeVersionEntity() {}

    public ResumeVersionEntity(ResumeVersionId id, int schemaVersion, String documentJson, Instant createdAt) {
        this.id = id;
        this.schemaVersion = schemaVersion;
        this.documentJson = documentJson;
        this.actor = "system";
        this.createdAt = createdAt;
    }

    public ResumeVersionId id() { return id; }
    public String documentJson() { return documentJson; }
}
