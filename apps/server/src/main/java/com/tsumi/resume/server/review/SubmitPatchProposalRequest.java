package com.tsumi.resume.server.review;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SubmitPatchProposalRequest(
        @NotNull JsonNode proposal,
        @NotNull @Valid PatchAssessmentRequest assessment) {}
