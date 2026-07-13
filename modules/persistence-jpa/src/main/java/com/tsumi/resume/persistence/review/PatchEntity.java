package com.tsumi.resume.persistence.review;

import com.tsumi.resume.domain.patch.ResumePatch;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "resume_patch")
public class PatchEntity {
    @EmbeddedId private PatchId id;
    @Column(name = "resume_id", nullable = false, length = 80) private String resumeId;
    @Column(name = "base_version", nullable = false) private long baseVersion;
    @Column(name = "patch_json", nullable = false, columnDefinition = "text") private String patchJson;
    @Column(name = "policy_decision", nullable = false, length = 24) private String policyDecision;
    @Column(name = "review_status", nullable = false, length = 24) private String reviewStatus;
    @Version @Column(name = "lock_version", nullable = false) private long lockVersion;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected PatchEntity() {}
    public PatchEntity(ResumePatch patch, String json, Instant now) {
        this.id = new PatchId(patch.taskId(), patch.patchId());
        this.resumeId = patch.resumeId();
        this.baseVersion = patch.baseVersion();
        this.createdAt = now;
        update(patch, json, now);
    }
    public void update(ResumePatch patch, String json, Instant now) {
        this.patchJson = json;
        this.policyDecision = patch.policyDecision().name();
        this.reviewStatus = patch.reviewStatus().name();
        this.updatedAt = now;
    }
    public String patchJson() { return patchJson; }
}
