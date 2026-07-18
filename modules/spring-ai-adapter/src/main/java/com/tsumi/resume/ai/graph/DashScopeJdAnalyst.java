package com.tsumi.resume.ai.graph;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DashScopeJdAnalyst implements StructuredJdAnalyst {

    private static final Map<String, Object> SCHEMA = schema();

    private final DashScopeStructuredModelClient client;
    private final String model;

    public DashScopeJdAnalyst(DashScopeStructuredModelClient client, String model) {
        this.client = client;
        this.model = model;
    }

    private static Map<String, Object> schema() {
        var fields = new LinkedHashMap<String, Object>();
        fields.put("requirementId", Map.of("type", "string"));
        fields.put("capability", Map.of("type", "string"));
        fields.put("sourceText", Map.of("type", "string"));
        fields.put("sourceStart", Map.of("type", "integer", "minimum", 0));
        fields.put("sourceEnd", Map.of("type", "integer", "minimum", 1));
        var item = new LinkedHashMap<String, Object>();
        item.put("type", "object");
        item.put("additionalProperties", false);
        item.put("required", List.of("requirementId", "capability", "sourceText", "sourceStart", "sourceEnd"));
        item.put("properties", fields);
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("requirements"),
                "properties", Map.of("requirements", Map.of("type", "array", "items", item)));
    }

    @Override
    public CapabilityMatrix analyze(String jobDescription) {
        var matrix = client.call(
                "jd_analyst",
                "jd-analyst/v1",
                "只提取 JD 原文明确表达的能力，并返回精确字符区间；不得补充 JD 中不存在的要求。",
                Map.of("jobDescription", jobDescription),
                CapabilityMatrix.class,
                SCHEMA,
                model,
                0.0);
        var normalized = matrix.requirements().stream()
                .map(requirement -> normalizeSourceSpan(jobDescription, requirement))
                .toList();
        return new CapabilityMatrix(normalized);
    }

    private CapabilityRequirement normalizeSourceSpan(
            String jobDescription,
            CapabilityRequirement requirement) {
        if (requirement.sourceEnd() <= jobDescription.length()
                && jobDescription.substring(requirement.sourceStart(), requirement.sourceEnd())
                        .equals(requirement.sourceText())) {
            return requirement;
        }
        var sourceStart = jobDescription.indexOf(requirement.sourceText());
        if (sourceStart < 0) {
            throw new ModelOutputRejectedException("jd_analyst", "source text does not occur in JD");
        }
        return new CapabilityRequirement(
                requirement.requirementId(),
                requirement.capability(),
                requirement.sourceText(),
                sourceStart,
                sourceStart + requirement.sourceText().length());
    }
}
