package com.tsumi.resume.workflow.review;

import com.tsumi.resume.domain.evidence.EvidenceApprovalStatus;
import java.time.Instant;

public record EvidenceView(
        String artifactId,
        String sourceType,
        String sourceRef,
        String excerpt,
        String contentHash,
        EvidenceApprovalStatus approvalStatus,
        Instant expiresAt) {}
