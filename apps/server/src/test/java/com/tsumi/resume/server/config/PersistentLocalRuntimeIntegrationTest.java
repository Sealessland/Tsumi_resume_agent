package com.tsumi.resume.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.workflow.ResumeAgentWorkflow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:local_profile;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("local")
class PersistentLocalRuntimeIntegrationTest {

    @Autowired TaskRepository tasks;
    @Autowired ResumeAgentWorkflow workflow;

    @Test
    void localProfileUsesJpaPersistenceAndDoesNotRequireDashScope() {
        assertThat(tasks.getClass().getName()).contains("JpaTaskRepositoryAdapter");
        assertThat(workflow.getClass().getName()).contains("LocalDeterministicWorkflow");
    }
}
