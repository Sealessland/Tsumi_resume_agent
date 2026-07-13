package com.tsumi.resume.server.config;

import com.tsumi.resume.infrastructure.contract.JsonContractValidator;
import com.tsumi.resume.infrastructure.resume.InMemoryResumeVersionStore;
import com.tsumi.resume.infrastructure.review.InMemoryPatchStore;
import com.tsumi.resume.infrastructure.task.InMemoryTaskRepository;
import com.tsumi.resume.infrastructure.workflow.LocalDeterministicWorkflow;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.workflow.ResumeAgentWorkflow;
import com.tsumi.resume.workflow.TaskOrchestrator;
import com.tsumi.resume.workflow.resume.ResumeVersionStore;
import com.tsumi.resume.workflow.resume.ResumeVersionReader;
import com.tsumi.resume.workflow.resume.VersionedResumeService;
import com.tsumi.resume.workflow.review.PatchStore;
import com.tsumi.resume.workflow.review.ResumeReviewService;
import java.io.IOException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalRuntimeConfiguration {

    @Bean
    TaskRepository taskRepository() {
        return new InMemoryTaskRepository();
    }

    @Bean
    ResumeAgentWorkflow resumeAgentWorkflow() {
        return new LocalDeterministicWorkflow();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ResumeVersionStore resumeVersionStore() {
        return new InMemoryResumeVersionStore();
    }

    @Bean
    VersionedResumeService versionedResumeService(ResumeVersionStore store) {
        return new VersionedResumeService(store);
    }

    @Bean
    PatchStore patchStore() {
        return new InMemoryPatchStore();
    }

    @Bean
    ResumeReviewService resumeReviewService(
            TaskRepository taskRepository,
            PatchStore patchStore,
            VersionedResumeService resumeService,
            Clock clock) {
        return new ResumeReviewService(taskRepository, patchStore, resumeService, clock);
    }

    @Bean("resumeContractValidator")
    JsonContractValidator resumeContractValidator() throws IOException {
        return packagedContract("contracts/resume.schema.json");
    }

    @Bean("patchProposalContractValidator")
    JsonContractValidator patchProposalContractValidator() throws IOException {
        return packagedContract("contracts/resume-patch-proposal.schema.json");
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

    private JsonContractValidator packagedContract(String path) throws IOException {
        return new JsonContractValidator(new ClassPathResource(path).getInputStream());
    }
}
