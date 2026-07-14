package com.tsumi.resume.server.config;

import com.tsumi.resume.infrastructure.contract.JsonContractValidator;
import com.tsumi.resume.persistence.config.PersistenceJpaConfiguration;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.task.TaskEventStore;
import com.tsumi.resume.workflow.ResumeAgentWorkflow;
import com.tsumi.resume.workflow.TaskOrchestrator;
import com.tsumi.resume.workflow.UnitOfWork;
import com.tsumi.resume.workflow.evidence.ClaimSupportEvaluator;
import com.tsumi.resume.workflow.evidence.EvidenceArtifactStore;
import com.tsumi.resume.workflow.evidence.EvidenceGuard;
import com.tsumi.resume.workflow.evidence.ServerEvidenceGuard;
import com.tsumi.resume.workflow.resume.ResumeVersionReader;
import com.tsumi.resume.workflow.resume.ResumeVersionStore;
import com.tsumi.resume.workflow.resume.VersionedResumeService;
import com.tsumi.resume.workflow.review.PatchStore;
import com.tsumi.resume.workflow.review.ResumeReviewService;
import java.io.IOException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

@Configuration(proxyBeanMethods = false)
@Profile({"local", "ai", "prod"})
@Import(PersistenceJpaConfiguration.class)
public class PersistentRuntimeConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    VersionedResumeService versionedResumeService(ResumeVersionStore store) {
        return new VersionedResumeService(store);
    }

    @Bean
    EvidenceGuard evidenceGuard(
            ResumeVersionReader resumes,
            ClaimSupportEvaluator evaluator,
            Clock clock) {
        return new ServerEvidenceGuard(resumes, evaluator, clock);
    }

    @Bean
    ResumeReviewService resumeReviewService(
            TaskRepository taskRepository,
            PatchStore patchStore,
            VersionedResumeService resumeService,
            Clock clock,
            EvidenceArtifactStore evidenceStore,
            EvidenceGuard evidenceGuard,
            UnitOfWork unitOfWork,
            TaskEventStore taskEvents) {
        return new ResumeReviewService(
                taskRepository, patchStore, resumeService, clock,
                evidenceStore, evidenceGuard, unitOfWork, taskEvents);
    }

    @Bean
    TaskOrchestrator taskOrchestrator(
            TaskRepository taskRepository,
            ResumeVersionReader resumeVersionReader,
            ResumeAgentWorkflow workflow,
            Clock clock) {
        return new TaskOrchestrator(
                taskRepository,
                resumeVersionReader,
                workflow,
                clock,
                () -> "task_" + UUID.randomUUID().toString().replace("-", ""));
    }

    @Bean("resumeContractValidator")
    JsonContractValidator resumeContractValidator() throws IOException {
        return packagedContract("contracts/resume.schema.json");
    }

    @Bean("patchProposalContractValidator")
    JsonContractValidator patchProposalContractValidator() throws IOException {
        return packagedContract("contracts/resume-patch-proposal.schema.json");
    }

    private JsonContractValidator packagedContract(String path) throws IOException {
        return new JsonContractValidator(new ClassPathResource(path).getInputStream());
    }
}
