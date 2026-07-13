package com.tsumi.resume.domain.merge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsumi.resume.domain.patch.PatchIntent;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.patch.ResumePatch;
import com.tsumi.resume.domain.patch.ReviewStatus;
import com.tsumi.resume.domain.policy.PatchAssessment;
import com.tsumi.resume.domain.policy.PatchPolicy;
import com.tsumi.resume.domain.policy.PatchProposal;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumePatchEngineTest {

    private final Path contracts = Path.of(System.getProperty("contracts.dir"));
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResumePatchEngine engine = new ResumePatchEngine();

    @Test
    void appliesAcceptedPatchThroughStableEntityIdWithoutMutatingBaseVersion() throws Exception {
        var source = fixture();

        var result = engine.apply(source, acceptedReplacePatch());

        assertThat(result.path("version").asLong()).isEqualTo(2);
        assertThat(result.at("/projects/0/description").asText())
                .isEqualTo("打通结构化编辑、实时预览及 PDF/PNG 导出链路。");
        assertThat(source.path("version").asLong()).isEqualTo(1);
        assertThat(source.at("/projects/0/description").asText())
                .isEqualTo("实现简历编辑和导出功能。");
    }

    @Test
    void revertsAsANewImmutableVersion() throws Exception {
        var patch = acceptedReplacePatch();
        var applied = engine.apply(fixture(), patch);

        var reverted = engine.revert(applied, patch);

        assertThat(reverted.path("version").asLong()).isEqualTo(3);
        assertThat(reverted.at("/projects/0/description").asText())
                .isEqualTo("实现简历编辑和导出功能。");
        assertThat(applied.at("/projects/0/description").asText())
                .isEqualTo("打通结构化编辑、实时预览及 PDF/PNG 导出链路。");
    }

    @Test
    void appliesAndRevertsRemoveWithoutInventingAValue() throws Exception {
        var patch = acceptedRemovePatch();

        var applied = engine.apply(fixture(), patch);
        var reverted = engine.revert(applied, patch);

        assertThat(applied.at("/projects/0").has("description")).isFalse();
        assertThat(reverted.at("/projects/0/description").asText())
                .isEqualTo("实现简历编辑和导出功能。");
        assertThat(reverted.path("version").asLong()).isEqualTo(3);
    }

    @Test
    void rejectsStaleBaseVersion() throws Exception {
        var source = fixture();
        source.put("version", 2);

        assertThatThrownBy(() -> engine.apply(source, acceptedReplacePatch()))
                .isInstanceOf(VersionConflictException.class)
                .hasMessageContaining("expected 1")
                .hasMessageContaining("was 2");
    }

    @Test
    void rejectsWhenBeforeValueNoLongerMatches() throws Exception {
        var source = fixture();
        ((ObjectNode) source.at("/projects/0")).put("description", "用户已经手工修改");

        assertThatThrownBy(() -> engine.apply(source, acceptedReplacePatch()))
                .isInstanceOf(PatchConflictException.class)
                .hasMessageContaining("before value");
    }

    @Test
    void rejectsPolicyAllowedPatchUntilHumanAcceptsIt() throws Exception {
        var pending = guardedReplacePatch();

        assertThatThrownBy(() -> engine.apply(fixture(), pending))
                .isInstanceOf(PatchConflictException.class)
                .hasMessageContaining("ACCEPTED");
    }

    @Test
    void rejectsUnknownStableEntityId() throws Exception {
        var proposal = replaceProposal("/projects/project_missing/description");
        var patch = new PatchPolicy().evaluate(
                proposal, new PatchAssessment(1.0, List.of(), List.of()))
                .patch().orElseThrow().reviewedAs(ReviewStatus.ACCEPTED);

        assertThatThrownBy(() -> engine.apply(fixture(), patch))
                .isInstanceOf(PatchConflictException.class)
                .hasMessageContaining("project_missing");
    }

    @Test
    void rejectsChangesToStableEntityIds() throws Exception {
        var proposal = new PatchProposal(
                "rp_03", "task_01", "res_fixture", 1, PatchOperation.REPLACE,
                "/projects/project_01/id", "project_01", "project_changed",
                PatchIntent.PARAPHRASE,
                List.of("resume:projects/project_01"), List.of(), 0.9);
        var patch = new PatchPolicy().evaluate(
                proposal, new PatchAssessment(1.0, List.of(), List.of()))
                .patch().orElseThrow().reviewedAs(ReviewStatus.ACCEPTED);

        assertThatThrownBy(() -> engine.apply(fixture(), patch))
                .isInstanceOf(PatchConflictException.class)
                .hasMessageContaining("protected");
    }

    private ObjectNode fixture() throws Exception {
        return (ObjectNode) objectMapper.readTree(
                contracts.resolve("fixtures/resume/valid-minimal-v13.json").toFile());
    }

    private ResumePatch acceptedReplacePatch() {
        return guardedReplacePatch().reviewedAs(ReviewStatus.ACCEPTED);
    }

    private ResumePatch guardedReplacePatch() {
        return new PatchPolicy().evaluate(
                replaceProposal("/projects/project_01/description"),
                new PatchAssessment(1.0, List.of(), List.of()))
                .patch().orElseThrow();
    }

    private PatchProposal replaceProposal(String path) {
        return new PatchProposal(
                "rp_01", "task_01", "res_fixture", 1, PatchOperation.REPLACE,
                path,
                "实现简历编辑和导出功能。",
                "打通结构化编辑、实时预览及 PDF/PNG 导出链路。",
                PatchIntent.PARAPHRASE,
                List.of("resume:projects/project_01"),
                List.of("jd:delivery/export"),
                0.92);
    }

    private ResumePatch acceptedRemovePatch() {
        var proposal = new PatchProposal(
                "rp_02", "task_01", "res_fixture", 1, PatchOperation.REMOVE,
                "/projects/project_01/description",
                "实现简历编辑和导出功能。", "", PatchIntent.DELETE,
                List.of("resume:projects/project_01"), List.of(), 0.9);
        return new PatchPolicy().evaluate(
                proposal, new PatchAssessment(1.0, List.of(), List.of()))
                .patch().orElseThrow().reviewedAs(ReviewStatus.ACCEPTED);
    }
}
