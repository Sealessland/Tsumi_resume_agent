package com.tsumi.resume.ai.graph;

public record CapabilityRequirement(
        String requirementId,
        String capability,
        String sourceText,
        int sourceStart,
        int sourceEnd) {

    public CapabilityRequirement {
        requireText(requirementId, "requirementId");
        requireText(capability, "capability");
        requireText(sourceText, "sourceText");
        if (sourceStart < 0 || sourceEnd <= sourceStart) {
            throw new IllegalArgumentException("source span must be non-empty");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }
}
