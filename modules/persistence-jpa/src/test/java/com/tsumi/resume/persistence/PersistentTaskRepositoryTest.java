package com.tsumi.resume.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tsumi.resume.persistence.config.PersistenceJpaConfiguration;
import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskRepository;
import com.tsumi.resume.task.TaskStatus;
import com.tsumi.resume.task.TaskVersionConflictException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = PersistentTaskRepositoryTest.TestApplication.class,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:task-store;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.flyway.enabled=true"
        })
@ActiveProfiles("test")
class PersistentTaskRepositoryTest {

    @Autowired
    private TaskRepository repository;

    @Test
    void roundTripsTheCompleteTaskAggregate() {
        var now = Instant.parse("2026-07-13T00:00:00Z");
        var task = ResumeTask.created(
                        "task_persist_01", "res_01", 1, "trace_01", now)
                .analyze(now)
                .lease("instance_01", now.plusSeconds(30), now);

        repository.save(ResumeTask.created(
                "task_persist_01", "res_01", 1, "trace_01", now));
        repository.save(task);

        var restored = repository.findById("task_persist_01").orElseThrow();
        assertThat(restored).isEqualTo(task);
        assertThat(restored.status()).isEqualTo(TaskStatus.ANALYZING);
    }

    @Test
    void rejectsAStaleDomainRevision() {
        var now = Instant.parse("2026-07-13T00:01:00Z");
        var created = ResumeTask.created(
                "task_persist_02", "res_01", 1, "trace_02", now);
        repository.save(created);
        repository.save(created.analyze(now));

        assertThatThrownBy(() -> repository.save(created.analyze(now.plusSeconds(1))))
                .isInstanceOf(TaskVersionConflictException.class);
    }

    @Test
    void findsCreatedAndExpiredLeaseTasksForRestartRecoveryOnly() {
        var now = Instant.parse("2026-07-14T00:00:00Z");
        repository.save(ResumeTask.created("task_recover_created", "res_01", 1, "trace_rc", now));
        var expired = ResumeTask.created("task_recover_expired", "res_01", 1, "trace_re", now)
                .analyze(now).lease("dead_worker", now.minusSeconds(1), now.minusSeconds(2));
        repository.save(ResumeTask.created("task_recover_expired", "res_01", 1, "trace_re", now));
        repository.save(expired);
        var live = ResumeTask.created("task_recover_live", "res_01", 1, "trace_rl", now)
                .analyze(now).lease("live_worker", now.plusSeconds(60), now);
        repository.save(ResumeTask.created("task_recover_live", "res_01", 1, "trace_rl", now));
        repository.save(live);

        assertThat(repository.findRecoverable(now, 20))
                .extracting(ResumeTask::taskId)
                .contains("task_recover_created", "task_recover_expired")
                .doesNotContain("task_recover_live");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(PersistenceJpaConfiguration.class)
    static class TestApplication {}
}
