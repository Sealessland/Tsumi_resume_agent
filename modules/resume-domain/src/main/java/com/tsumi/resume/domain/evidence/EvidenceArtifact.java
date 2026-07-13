package com.tsumi.resume.domain.evidence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

public record EvidenceArtifact(
        String artifactId,
        String taskId,
        String resumeId,
        String sourceType,
        String sourceRef,
        String excerpt,
        String contentHash,
        EvidenceApprovalStatus approvalStatus,
        Instant expiresAt,
        Instant createdAt) {

    public EvidenceArtifact {
        requireText(artifactId, "artifactId");
        requireText(taskId, "taskId");
        requireText(resumeId, "resumeId");
        requireText(sourceType, "sourceType");
        requireText(sourceRef, "sourceRef");
        requireText(excerpt, "excerpt");
        requireText(contentHash, "contentHash");
        Objects.requireNonNull(approvalStatus, "approvalStatus");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static EvidenceArtifact approved(
            String artifactId,
            String taskId,
            String resumeId,
            String sourceType,
            String sourceRef,
            String excerpt,
            Instant expiresAt,
            Instant createdAt) {
        return new EvidenceArtifact(
                artifactId, taskId, resumeId, sourceType, sourceRef, excerpt,
                sha256(excerpt), EvidenceApprovalStatus.APPROVED, expiresAt, createdAt);
    }

    public boolean hasValidHash() {
        return MessageDigest.isEqual(
                contentHash.getBytes(StandardCharsets.US_ASCII),
                sha256(excerpt).getBytes(StandardCharsets.US_ASCII));
    }

    public boolean isUsableAt(Instant now) {
        return approvalStatus == EvidenceApprovalStatus.APPROVED
                && (expiresAt == null || expiresAt.isAfter(now))
                && hasValidHash();
    }

    public static String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
