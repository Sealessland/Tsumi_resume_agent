package com.tsumi.resume.server.review;

import com.tsumi.resume.domain.policy.PatchAssessment;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PatchAssessmentRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double evidenceCoverage,
        @NotNull List<String> newAtomicClaims,
        @NotNull List<String> riskFlags) {

    PatchAssessment toDomain() {
        return new PatchAssessment(evidenceCoverage, newAtomicClaims, riskFlags);
    }
}
