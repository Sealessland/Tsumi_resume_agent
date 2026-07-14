package com.tsumi.resume.ai.graph;

import java.util.List;

public record CapabilityMatrix(List<CapabilityRequirement> requirements) {
    public CapabilityMatrix {
        requirements = List.copyOf(requirements);
    }
}
