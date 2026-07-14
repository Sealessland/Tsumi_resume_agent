package com.tsumi.resume.ai.graph;

import com.tsumi.resume.domain.patch.PatchIntent;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class DashScopeResumeRewriter implements StructuredResumeRewriter {

    private static final Map<String, Object> SCHEMA = schema();

    private final DashScopeStructuredModelClient client;
    private final EvidenceArtifactStore evidenceStore;
    private final Clock clock;
    private final String model;
    private final Supplier<String> patchIdSupplier;

    public DashScopeResumeRewriter(
            DashScopeStructuredModelClient client,
            EvidenceArtifactStore evidenceStore,
            Clock clock,
            String model,
            Supplier<String> patchIdSupplier) {
        this.client = client;
        this.evidenceStore = evidenceStore;
        this.clock = clock;
        this.model = model;
        this.patchIdSupplier = patchIdSupplier;
    }

    @Override
    public List<PatchProposal> propose(RewriteRequest request) {
        var evidence = evidenceStore.findByTaskId(request.workflow().taskId()).stream()
                .filter(artifact -> artifact.isUsableAt(clock.instant()))
                .map(artifact -> new EvidenceView(
                        artifact.artifactId(), artifact.sourceType(), artifact.sourceRef(), artifact.excerpt()))
                .toList();
        var input = new LinkedHashMap<String, Object>();
        input.put("resume", request.resume().content());
        input.put("capabilityMatrix", request.capabilityMatrix());
        input.put("approvedEvidence", evidence);
        input.put("coverageGaps", request.gaps());
        input.put("repair", request.repair());
        input.put("constraints", List.of(
                "after 中的事实必须能在 before 或 approvedEvidence 原文定位",
                "没有权威数据时禁止生成数字、百分比、日期、金额或专有名词",
                "只返回路径级修改，不返回整份简历"));
        var batch = client.call(
                "rewrite_agent",
                request.repair() ? "resume-rewrite-repair/v1" : "resume-rewrite/v1",
                "只能改写已有且可验证的事实。不得猜测指标、ATS 分数、成本或业绩。",
                input,
                ModelProposalBatch.class,
                SCHEMA,
                model,
                0.2);
        var result = new ArrayList<PatchProposal>();
        for (var proposed : batch.proposals()) {
            proposed.validate();
            result.add(new PatchProposal(
                    patchIdSupplier.get(),
                    request.workflow().taskId(),
                    request.workflow().resumeId(),
                    request.workflow().baseVersion(),
                    proposed.op() == null ? PatchOperation.REPLACE : proposed.op(),
                    proposed.path(), proposed.before(), proposed.after(), proposed.intent(),
                    proposed.evidenceRefs(), proposed.jdRefs(), proposed.confidence()));
        }
        return List.copyOf(result);
    }

    private static Map<String, Object> schema() {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("op", Map.of("type", "string", "enum", List.of("replace", "remove")));
        properties.put("path", Map.of("type", "string", "pattern", "^/"));
        properties.put("before", Map.of("type", "string"));
        properties.put("after", Map.of("type", "string"));
        properties.put("intent", Map.of("type", "string", "enum", java.util.Arrays.stream(PatchIntent.values()).map(Enum::name).toList()));
        properties.put("evidenceRefs", Map.of("type", "array", "items", Map.of("type", "string")));
        properties.put("jdRefs", Map.of("type", "array", "items", Map.of("type", "string")));
        properties.put("confidence", Map.of("type", "number", "minimum", 0, "maximum", 1));
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("proposals"),
                "properties", Map.of("proposals", Map.of(
                        "type", "array",
                        "maxItems", 20,
                        "items", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "required", List.of("path", "before", "after", "intent", "evidenceRefs", "jdRefs", "confidence"),
                                "properties", properties))));
    }

    public record ModelProposalBatch(List<ModelProposal> proposals) {
        public ModelProposalBatch {
            proposals = List.copyOf(proposals);
            if (proposals.size() > 20) throw new IllegalArgumentException("too many proposals");
        }
    }

    public record ModelProposal(
            PatchOperation op,
            String path,
            String before,
            String after,
            PatchIntent intent,
            List<String> evidenceRefs,
            List<String> jdRefs,
            double confidence) {
        public ModelProposal {
            evidenceRefs = List.copyOf(evidenceRefs);
            jdRefs = List.copyOf(jdRefs);
        }

        void validate() {
            if (path == null || !path.startsWith("/")) throw new ModelOutputRejectedException("rewrite_agent", "invalid path");
            if (before == null || after == null || intent == null) throw new ModelOutputRejectedException("rewrite_agent", "missing proposal field");
            if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
                throw new ModelOutputRejectedException("rewrite_agent", "invalid confidence");
            }
        }
    }

    private record EvidenceView(String artifactId, String sourceType, String sourceRef, String excerpt) {}
}
