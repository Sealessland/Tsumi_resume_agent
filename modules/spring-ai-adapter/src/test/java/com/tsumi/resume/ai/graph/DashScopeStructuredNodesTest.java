package com.tsumi.resume.ai.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import com.tsumi.resume.workflow.WorkflowInput;
import com.tsumi.resume.workflow.WorkflowExecutionException;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class DashScopeStructuredNodesTest {

    @Test
    void analystUsesVersionedStrictJsonSchemaAndDeterministicOptions() {
        var model = new RecordingChatModel("""
                {"requirements":[{"requirementId":"jd_01","capability":"Spring Boot",
                "sourceText":"Spring Boot","sourceStart":3,"sourceEnd":14}]}
                """);
        var client = new DashScopeStructuredModelClient(
                model, new ObjectMapper(), Duration.ofSeconds(1), 1_000);

        var matrix = new DashScopeJdAnalyst(client, "qwen-plus").analyze("需要 Spring Boot 经验");

        assertThat(matrix.requirements()).hasSize(1);
        assertThat(matrix.requirements().getFirst().sourceText()).isEqualTo("Spring Boot");
        var prompt = model.prompts.getFirst();
        assertThat(prompt.getSystemMessage().getText()).contains("jd-analyst/v1", "不得补充 JD 中不存在的要求");
        var options = (DashScopeChatOptions) prompt.getOptions();
        assertThat(options.getModel()).isEqualTo("qwen-plus");
        assertThat(options.getTemperature()).isZero();
        assertThat(options.getMaxInputTokens()).isEqualTo(12_000);
        assertThat(options.getMaxTokens()).isEqualTo(1_000);
        assertThat(options.getResponseFormat().getType()).isEqualTo(DashScopeResponseFormat.Type.JSON_SCHEMA);
        assertThat(options.getResponseFormat().getJsonScheme().getStrict()).isTrue();
    }

    @Test
    void rejectsUnknownFieldsInsteadOfSilentlyAcceptingModelDrift() {
        var model = new RecordingChatModel("""
                {"requirements":[],"inventedScore":99}
                """);
        var client = new DashScopeStructuredModelClient(
                model, new ObjectMapper(), Duration.ofSeconds(1), 1_000);

        assertThatThrownBy(() -> new DashScopeJdAnalyst(client, "qwen-plus").analyze("Java"))
                .isInstanceOf(ModelOutputRejectedException.class)
                .isInstanceOf(WorkflowExecutionException.class)
                .hasMessageContaining("jd_analyst");
    }

    @Test
    void rewriteReceivesOnlyUsableEvidenceAndServerOwnsPatchIdentity() throws Exception {
        var model = new RecordingChatModel("""
                {"proposals":[{"path":"/basics/summary","before":"负责后端开发",
                "after":"使用 Spring Boot 负责后端开发","intent":"PARAPHRASE",
                "evidenceRefs":["evidence_01"],"jdRefs":["jd_01"],"confidence":0.8}]}
                """);
        var now = Instant.parse("2026-07-14T00:00:00Z");
        var approved = EvidenceArtifact.approved(
                "evidence_01", "task_01", "resume_01", "user_note", "note_01",
                "项目使用 Spring Boot", now.plusSeconds(60), now);
        var expired = EvidenceArtifact.approved(
                "evidence_02", "task_01", "resume_01", "user_note", "note_02",
                "过期材料", now.minusSeconds(1), now.minusSeconds(60));
        var store = new FixedEvidenceStore(List.of(approved, expired));
        var client = new DashScopeStructuredModelClient(
                model, new ObjectMapper(), Duration.ofSeconds(1), 1_000);
        var rewriter = new DashScopeResumeRewriter(
                client, store, Clock.fixed(now, ZoneOffset.UTC), "qwen-plus", () -> "patch_server_01");
        var content = (com.fasterxml.jackson.databind.node.ObjectNode) new ObjectMapper().readTree("""
                {"schemaVersion":13,"resumeId":"resume_01","version":1,
                "basics":{"summary":"负责后端开发"}}
                """);
        var input = new WorkflowInput("task_01", "resume_01", 1, "需要 Spring Boot");
        var request = new RewriteRequest(
                input, new ResumeModelView("resume_01", 1, content),
                new CapabilityMatrix(List.of(new CapabilityRequirement(
                        "jd_01", "Spring Boot", "Spring Boot", 3, 14))), List.of(), false);

        var proposals = rewriter.propose(request);

        assertThat(proposals).singleElement().satisfies(proposal -> {
            assertThat(proposal.patchId()).isEqualTo("patch_server_01");
            assertThat(proposal.taskId()).isEqualTo("task_01");
            assertThat(proposal.resumeId()).isEqualTo("resume_01");
        });
        assertThat(model.prompts.getFirst().getUserMessage().getText())
                .contains("evidence_01", "项目使用 Spring Boot")
                .doesNotContain("evidence_02", "过期材料");
        assertThat(((DashScopeChatOptions) model.prompts.getFirst().getOptions()).getTemperature())
                .isEqualTo(0.2);
    }

    @Test
    void retriesProviderFailureOnlyOnceAndReturnsSanitizedRetryableCode() {
        var attempts = new AtomicInteger();
        ChatModel unavailable = prompt -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("provider secret response");
        };
        var client = new DashScopeStructuredModelClient(
                unavailable, new ObjectMapper(), Duration.ofSeconds(1), 1_000);

        assertThatThrownBy(() -> new DashScopeJdAnalyst(client, "qwen-plus").analyze("Java"))
                .isInstanceOf(WorkflowExecutionException.class)
                .satisfies(error -> {
                    var controlled = (WorkflowExecutionException) error;
                    assertThat(controlled.code()).isEqualTo("MODEL_PROVIDER_UNAVAILABLE");
                    assertThat(controlled.retryable()).isTrue();
                });
        assertThat(attempts).hasValue(2);
    }

    private static final class RecordingChatModel implements ChatModel {
        private final ArrayDeque<String> responses = new ArrayDeque<>();
        private final java.util.ArrayList<Prompt> prompts = new java.util.ArrayList<>();

        private RecordingChatModel(String... responses) {
            this.responses.addAll(List.of(responses));
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(responses.removeFirst()))));
        }
    }

    private static final class FixedEvidenceStore implements EvidenceArtifactStore {
        private final List<EvidenceArtifact> artifacts;

        private FixedEvidenceStore(List<EvidenceArtifact> artifacts) {
            this.artifacts = artifacts;
        }

        @Override public EvidenceArtifact save(EvidenceArtifact artifact) { throw new UnsupportedOperationException(); }
        @Override public Optional<EvidenceArtifact> findById(String artifactId) { return Optional.empty(); }
        @Override public List<EvidenceArtifact> findByTaskId(String taskId) { return artifacts; }
    }
}
