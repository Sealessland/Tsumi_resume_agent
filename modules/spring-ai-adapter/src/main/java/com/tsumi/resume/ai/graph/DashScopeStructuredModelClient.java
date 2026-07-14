package com.tsumi.resume.ai.graph;

import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

public final class DashScopeStructuredModelClient {

    private static final int MAX_INPUT_TOKENS = 12_000;
    private final ChatModel model;
    private final ObjectMapper mapper;
    private final Duration timeout;
    private final int maxOutputTokens;

    public DashScopeStructuredModelClient(
            ChatModel model,
            ObjectMapper mapper,
            Duration timeout,
            int maxOutputTokens) {
        this.model = model;
        this.mapper = mapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.timeout = timeout;
        this.maxOutputTokens = maxOutputTokens;
    }

    public <T> T call(
            String node,
            String promptVersion,
            String policy,
            Object input,
            Class<T> outputType,
            Object jsonSchema,
            String modelName,
            double temperature) {
        var responseFormat = DashScopeResponseFormat.builder()
                .type(DashScopeResponseFormat.Type.JSON_SCHEMA)
                .jsonScheme(DashScopeResponseFormat.JsonSchemaConfig.builder()
                        .name(node + "_output")
                        .description("Strict structured output for " + node)
                        .schema(jsonSchema)
                        .strict(true)
                        .build())
                .build();
        var options = DashScopeChatOptions.builder()
                .model(modelName)
                .temperature(temperature)
                .maxToken(maxOutputTokens)
                .responseFormat(responseFormat)
                .build();
        options.setMaxInputTokens(MAX_INPUT_TOKENS);
        final String userJson;
        try {
            userJson = mapper.writeValueAsString(input);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot serialize structured input for " + node, exception);
        }
        var prompt = new Prompt(List.of(
                new SystemMessage("prompt=" + promptVersion + "\n" + policy),
                new UserMessage(userJson)), options);

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                var raw = invokeWithTimeout(node, prompt);
                return mapper.readValue(raw, outputType);
            } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
                throw new ModelOutputRejectedException(node, exception);
            } catch (WorkflowModelTimeoutException exception) {
                lastFailure = exception;
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }
        throw lastFailure;
    }

    private String invokeWithTimeout(String node, Prompt prompt) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var future = executor.submit(() -> model.call(prompt));
            var response = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new ModelOutputRejectedException(node, "empty response");
            }
            var text = response.getResult().getOutput().getText();
            if (text == null || text.isBlank()) {
                throw new ModelOutputRejectedException(node, "empty response body");
            }
            return text;
        } catch (TimeoutException exception) {
            throw new WorkflowModelTimeoutException(node, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Model call was interrupted for " + node, exception);
        } catch (ExecutionException exception) {
            var cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Model call failed for " + node, cause);
        }
    }
}
