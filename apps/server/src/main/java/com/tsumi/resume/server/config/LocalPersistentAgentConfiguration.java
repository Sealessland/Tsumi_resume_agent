package com.tsumi.resume.server.config;

import com.tsumi.resume.domain.evidence.ClaimAssessment;
import com.tsumi.resume.domain.evidence.ClaimVerdict;
import com.tsumi.resume.infrastructure.workflow.LocalDeterministicWorkflow;
import com.tsumi.resume.workflow.ResumeAgentWorkflow;
import com.tsumi.resume.workflow.evidence.ClaimSupportEvaluator;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("local")
public class LocalPersistentAgentConfiguration {

    @Bean
    ResumeAgentWorkflow resumeAgentWorkflow() {
        return new LocalDeterministicWorkflow();
    }

    @Bean
    ClaimSupportEvaluator claimSupportEvaluator() {
        return (before, after, evidence) -> List.of(new ClaimAssessment(
                after,
                evidence.isEmpty() ? ClaimVerdict.AMBIGUOUS : ClaimVerdict.SUPPORTED,
                evidence.stream().map(artifact -> artifact.artifactId()).toList(),
                evidence.isEmpty()
                        ? "No approved Evidence is available"
                        : "Deterministic local recording accepted the evidence-backed paraphrase"));
    }
}
