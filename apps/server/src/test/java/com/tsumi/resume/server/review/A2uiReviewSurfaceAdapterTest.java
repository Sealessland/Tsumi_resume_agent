package com.tsumi.resume.server.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsumi.resume.domain.evidence.ClaimAssessment;
import com.tsumi.resume.domain.evidence.ClaimVerdict;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.patch.PolicyDecision;
import com.tsumi.resume.domain.patch.ReviewStatus;
import com.tsumi.resume.workflow.review.AllowedAction;
import com.tsumi.resume.workflow.review.CostSummary;
import com.tsumi.resume.workflow.review.CoverageGap;
import com.tsumi.resume.workflow.review.PatchReviewItem;
import com.tsumi.resume.workflow.review.ReviewSurface;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class A2uiReviewSurfaceAdapterTest {

    @Test
    void producesAFramedV091StreamUsingOnlyTheFixedReviewCatalog() throws Exception {
        var mapper = new ObjectMapper().findAndRegisterModules();
        var adapter = new A2uiReviewSurfaceAdapter(mapper);
        var patch = new PatchReviewItem(
                "rp_01", "/projects/0/description", PatchOperation.REPLACE,
                "开发 Java 服务", "负责 Java 服务开发", List.of(),
                List.of(new ClaimAssessment(
                        "负责 Java 服务开发", ClaimVerdict.SUPPORTED,
                        List.of("ev_01"), "Supported by approved resume evidence")),
                PolicyDecision.ALLOW, List.of(), ReviewStatus.PENDING,
                List.of(AllowedAction.ACCEPT, AllowedAction.REJECT, AllowedAction.EDIT), 1);
        var surface = new ReviewSurface(
                "review_task_01_v1", "task_01", 1, List.of(patch),
                List.of(new CoverageGap(
                        "rp_blocked", "/projects/1/description",
                        List.of("性能提升 50%"), "Unsupported protected fact")),
                List.of(), List.of(AllowedAction.CANCEL),
                new CostSummary(null, null, null, null));

        var stream = adapter.toMessageStream(surface);
        var lines = stream.lines().toList();

        assertThat(lines).hasSize(3);
        assertThat(mapper.readTree(lines.get(0)).path("version").asText()).isEqualTo("v0.9.1");
        assertThat(mapper.readTree(lines.get(0)).path("createSurface").path("catalogId").asText())
                .isEqualTo("urn:tsumi:a2ui:resume-review:0.9.1");
        var componentMessage = mapper.readTree(lines.get(1));
        assertThat(componentMessage.path("updateComponents").path("components").get(0)
                .path("id").asText()).isEqualTo("root");
        var used = new HashSet<String>();
        componentMessage.path("updateComponents").path("components")
                .forEach(component -> used.add(component.path("component").asText()));
        assertThat(used).isSubsetOf(
                "DiffCard", "EvidenceList", "RiskBadge", "CoverageGap",
                "ReviewActions", "TaskTimeline", "CostSummary");
        assertThat(mapper.readTree(lines.get(2)).has("updateDataModel")).isTrue();
        assertThat(stream.getBytes(java.nio.charset.StandardCharsets.UTF_8).length)
                .isLessThanOrEqualTo(256 * 1024);
        assertThat(stream).doesNotContain("javascript:", "<script", "estimatedCost\":50");
    }
}
