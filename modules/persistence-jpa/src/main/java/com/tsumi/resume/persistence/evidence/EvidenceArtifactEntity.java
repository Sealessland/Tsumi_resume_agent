package com.tsumi.resume.persistence.evidence;

import com.tsumi.resume.domain.evidence.EvidenceApprovalStatus;
import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "evidence_artifact")
public class EvidenceArtifactEntity {
    @Id @Column(name = "artifact_id", length = 100) private String artifactId;
    @Column(name = "task_id", nullable = false, length = 80) private String taskId;
    @Column(name = "resume_id", nullable = false, length = 80) private String resumeId;
    @Column(name = "source_type", nullable = false, length = 40) private String sourceType;
    @Column(name = "source_ref", nullable = false, length = 512) private String sourceRef;
    @Column(name = "excerpt_text", nullable = false, columnDefinition = "text") private String excerpt;
    @Column(name = "content_hash", nullable = false, length = 96) private String contentHash;
    @Column(name = "approval_status", nullable = false, length = 24) private String approvalStatus;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected EvidenceArtifactEntity() {}
    public EvidenceArtifactEntity(EvidenceArtifact artifact) {
        artifactId = artifact.artifactId(); taskId = artifact.taskId(); resumeId = artifact.resumeId();
        sourceType = artifact.sourceType(); sourceRef = artifact.sourceRef(); excerpt = artifact.excerpt();
        contentHash = artifact.contentHash(); approvalStatus = artifact.approvalStatus().name();
        expiresAt = artifact.expiresAt(); createdAt = artifact.createdAt();
    }
    public EvidenceArtifact toDomain() {
        return new EvidenceArtifact(artifactId, taskId, resumeId, sourceType, sourceRef, excerpt,
                contentHash, EvidenceApprovalStatus.valueOf(approvalStatus), expiresAt, createdAt);
    }
}
