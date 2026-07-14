package com.tsumi.resume.ai.graph;

import com.tsumi.resume.domain.evidence.ClaimAssessment;
import com.tsumi.resume.domain.evidence.ClaimVerdict;
import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import com.tsumi.resume.workflow.evidence.ClaimSupportEvaluator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DashScopeClaimSupportEvaluator implements ClaimSupportEvaluator {

    private static final Map<String, Object> SCHEMA = schema();
    private final DashScopeStructuredModelClient client;
    private final String model;

    public DashScopeClaimSupportEvaluator(DashScopeStructuredModelClient client, String model) {
        this.client = client;
        this.model = model;
    }

    @Override
    public List<ClaimAssessment> evaluate(
            String before,
            String after,
            List<EvidenceArtifact> evidence) {
        var input = new LinkedHashMap<String, Object>();
        input.put("before", before);
        input.put("after", after);
        input.put("approvedEvidence", evidence.stream().map(artifact -> Map.of(
                "artifactId", artifact.artifactId(),
                "sourceType", artifact.sourceType(),
                "excerpt", artifact.excerpt())).toList());
        var batch = client.call(
                "evidence_guard",
                "evidence-claim-verifier/v1",
                "逐条拆分 after 的原子事实。只能引用 approvedEvidence；不能确定时必须返回 AMBIGUOUS，禁止猜测支持关系。",
                input,
                ClaimBatch.class,
                SCHEMA,
                model,
                0.0);
        return batch.claims();
    }

    private static Map<String, Object> schema() {
        var fields = new LinkedHashMap<String, Object>();
        fields.put("claim", Map.of("type", "string"));
        fields.put("verdict", Map.of(
                "type", "string", "enum", java.util.Arrays.stream(ClaimVerdict.values()).map(Enum::name).toList()));
        fields.put("evidenceRefs", Map.of("type", "array", "items", Map.of("type", "string")));
        fields.put("reason", Map.of("type", "string"));
        var claim = new LinkedHashMap<String, Object>();
        claim.put("type", "object");
        claim.put("additionalProperties", false);
        claim.put("required", List.of("claim", "verdict", "evidenceRefs", "reason"));
        claim.put("properties", fields);
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("claims"),
                "properties", Map.of("claims", Map.of(
                        "type", "array", "minItems", 1, "maxItems", 50, "items", claim)));
    }

    public record ClaimBatch(List<ClaimAssessment> claims) {
        public ClaimBatch {
            claims = List.copyOf(claims);
            if (claims.isEmpty() || claims.size() > 50) {
                throw new IllegalArgumentException("claim count is outside the server limit");
            }
        }
    }
}
