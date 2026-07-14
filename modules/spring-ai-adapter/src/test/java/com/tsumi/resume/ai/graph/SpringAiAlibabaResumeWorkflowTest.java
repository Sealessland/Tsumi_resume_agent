package com.tsumi.resume.ai.graph;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsumi.resume.domain.patch.PatchIntent;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.workflow.WorkflowInput;
import com.tsumi.resume.workflow.WorkflowObserver;
import com.tsumi.resume.task.WorkflowNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpringAiAlibabaResumeWorkflowTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void followsFixedGraphAndAllowsOnlyOneTargetedRepairBeforeHumanReview() throws Exception {
        var calls = new ArrayList<String>();
        var input = new WorkflowInput("task_01", "resume_01", 1, "需要 Spring Boot 与 PostgreSQL 经验");
        var resume = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree("""
                {
                  "schemaVersion": 13,
                  "resumeId": "resume_01",
                  "version": 1,
                  "basics": {
                    "name": "张三",
                    "email": "zhangsan@example.com",
                    "phone": "13800000000",
                    "summary": "负责后端开发"
                  }
                }
                """);

        StructuredJdAnalyst analyst = jd -> {
            calls.add("analyze");
            return new CapabilityMatrix(List.of(new CapabilityRequirement(
                    "jd_01", "Spring Boot", "Spring Boot", 3, 14)));
        };
        StructuredResumeRewriter rewriter = request -> {
            calls.add(request.repair() ? "rewrite:repair" : "rewrite:initial");
            var suffix = request.repair() ? "，使用 Spring Boot" : "，性能提升 50%";
            return List.of(proposal(input, suffix));
        };
        ProposalPreChecker preChecker = (workflowInput, modelView, proposals) -> {
            calls.add("precheck");
            return PrecheckResult.ready(proposals);
        };
        WorkflowEvidenceVerifier verifier = (workflowInput, proposals) -> {
            calls.add("verify");
            if (calls.stream().filter("verify"::equals).count() == 1) {
                return new VerificationResult(
                        List.of(),
                        List.of(new CoverageGap(
                                "patch_01", "/basics/summary", List.of("50%"),
                                "数字 50% 没有批准 Evidence 支持")));
            }
            return new VerificationResult(proposals, List.of());
        };
        VerifiedProposalSink sink = (taskId, proposals) -> calls.add("sink:" + proposals.size());

        var workflow = new SpringAiAlibabaResumeWorkflow(
                (resumeId, version) -> {
                    calls.add("load");
                    return resume.deepCopy();
                },
                new ResumeModelSanitizer(), analyst, rewriter, preChecker, verifier, sink);

        var observed = new ArrayList<String>();
        var result = workflow.execute(input, new WorkflowObserver() {
            @Override public void nodeStarted(WorkflowNode node) { observed.add("start:" + node); }
            @Override public void nodeCompleted(WorkflowNode node, long durationMillis) { observed.add("done:" + node); }
            @Override public void nodeFailed(WorkflowNode node, String errorCode, long durationMillis) {
                observed.add("failed:" + node);
            }
        });

        assertThat(calls).containsExactly(
                "load", "analyze", "rewrite:initial", "precheck", "verify",
                "rewrite:repair", "precheck", "verify", "sink:1");
        assertThat(result.summary()).isEqualTo("REVIEW_READY; proposals=1; gaps=0; repairs=1");
        assertThat(observed).containsSubsequence(
                "start:JD_ANALYST", "done:JD_ANALYST",
                "start:REWRITE_AGENT", "done:REWRITE_AGENT",
                "start:EVIDENCE_GUARD", "done:EVIDENCE_GUARD",
                "start:REPAIR_AGENT", "done:REPAIR_AGENT",
                "start:REVIEW_AGGREGATOR", "done:REVIEW_AGGREGATOR");
        assertThat(workflow.snapshot(input.taskId()).next()).isEqualTo("human_review");
    }

    @Test
    void stopsAfterSecondEvidenceFailureWithoutCreatingPlaceholderPatch() throws Exception {
        var input = new WorkflowInput("task_02", "resume_01", 1, "需要可核验的交付结果");
        var resume = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree("""
                {
                  "schemaVersion": 13,
                  "resumeId": "resume_01",
                  "version": 1,
                  "basics": {"summary": "负责后端开发"}
                }
                """);
        var sinkCalls = new ArrayList<List<PatchProposal>>();
        StructuredResumeRewriter rewriter = request -> List.of(proposal(input, "，性能提升 50%"));
        WorkflowEvidenceVerifier alwaysReject = (workflowInput, proposals) -> new VerificationResult(
                List.of(), List.of(new CoverageGap(
                        "patch_01", "/basics/summary", List.of("50%"), "没有批准 Evidence")));

        var workflow = new SpringAiAlibabaResumeWorkflow(
                (resumeId, version) -> resume.deepCopy(),
                new ResumeModelSanitizer(),
                jd -> new CapabilityMatrix(List.of()),
                rewriter,
                (workflowInput, modelView, proposals) -> PrecheckResult.ready(proposals),
                alwaysReject,
                (taskId, proposals) -> sinkCalls.add(proposals));

        var result = workflow.execute(input);

        assertThat(result.summary()).isEqualTo("REVIEW_READY; proposals=0; gaps=1; repairs=1");
        assertThat(sinkCalls).containsExactly(List.of());
    }

    private PatchProposal proposal(WorkflowInput input, String suffix) {
        return new PatchProposal(
                "patch_01", input.taskId(), input.resumeId(), input.baseVersion(),
                PatchOperation.REPLACE, "/basics/summary", "负责后端开发",
                "负责后端开发" + suffix, PatchIntent.PARAPHRASE,
                List.of("evidence_01"), List.of("jd_01"), 0.8);
    }
}
