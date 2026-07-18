package com.tsumi.resume.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsumi.resume.ai.graph.DashScopeClaimSupportEvaluator;
import com.tsumi.resume.ai.graph.DashScopeJdAnalyst;
import com.tsumi.resume.ai.graph.DashScopeResumeRewriter;
import com.tsumi.resume.ai.graph.DashScopeStructuredModelClient;
import com.tsumi.resume.ai.graph.DeterministicProposalPreChecker;
import com.tsumi.resume.ai.graph.ResumeModelSanitizer;
import com.tsumi.resume.ai.graph.ReviewServiceProposalSink;
import com.tsumi.resume.ai.graph.ServerWorkflowEvidenceVerifier;
import com.tsumi.resume.ai.graph.SpringAiAlibabaResumeWorkflow;
import com.tsumi.resume.ai.graph.StructuredJdAnalyst;
import com.tsumi.resume.ai.graph.StructuredResumeRewriter;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.workflow.ResumeAgentWorkflow;
import com.tsumi.resume.workflow.evidence.ClaimSupportEvaluator;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import com.tsumi.resume.workflow.evidence.EvidenceGuard;
import com.tsumi.resume.workflow.resume.ResumeVersionReader;
import com.tsumi.resume.workflow.review.ResumeReviewService;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile({"ai", "prod"})
public class AiAgentConfiguration {

    @Bean
    ChatModel chatModel(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model}") String model) {
        var api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        var options = OpenAiChatOptions.builder()
                .model(model)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }

    @Bean
    DashScopeStructuredModelClient structuredModelClient(
            ChatModel chatModel,
            ObjectMapper objectMapper,
            @Value("${tsumi.ai.node-timeout:30s}") Duration timeout,
            @Value("${tsumi.ai.max-output-tokens:2000}") int maxOutputTokens) {
        return new DashScopeStructuredModelClient(chatModel, objectMapper, timeout, maxOutputTokens);
    }

    @Bean
    StructuredJdAnalyst structuredJdAnalyst(
            DashScopeStructuredModelClient client,
            @Value("${tsumi.ai.analyst-model:qwen-plus}") String model) {
        return new DashScopeJdAnalyst(client, model);
    }

    @Bean
    StructuredResumeRewriter structuredResumeRewriter(
            DashScopeStructuredModelClient client,
            EvidenceArtifactStore evidenceStore,
            Clock clock,
            @Value("${tsumi.ai.rewrite-model:qwen-plus}") String model) {
        return new DashScopeResumeRewriter(
                client, evidenceStore, clock, model,
                () -> "patch_" + UUID.randomUUID().toString().replace("-", ""));
    }

    @Bean
    ClaimSupportEvaluator claimSupportEvaluator(
            DashScopeStructuredModelClient client,
            @Value("${tsumi.ai.guard-model:qwen-plus}") String model) {
        return new DashScopeClaimSupportEvaluator(client, model);
    }

    @Bean
    ResumeAgentWorkflow resumeAgentWorkflow(
            ResumeVersionReader resumes,
            StructuredJdAnalyst analyst,
            StructuredResumeRewriter rewriter,
            TaskRepository tasks,
            EvidenceArtifactStore evidenceStore,
            EvidenceGuard guard,
            ResumeReviewService reviews) {
        return new SpringAiAlibabaResumeWorkflow(
                resumes,
                new ResumeModelSanitizer(),
                analyst,
                rewriter,
                new DeterministicProposalPreChecker(),
                new ServerWorkflowEvidenceVerifier(tasks, evidenceStore, guard),
                new ReviewServiceProposalSink(reviews));
    }
}
