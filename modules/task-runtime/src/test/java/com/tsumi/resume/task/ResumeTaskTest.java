package com.tsumi.resume.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ResumeTaskTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-13T00:00:00Z");
    private static final Instant STARTED_AT = Instant.parse("2026-07-13T00:00:01Z");
    private static final Instant REVIEW_AT = Instant.parse("2026-07-13T00:00:02Z");

    @Test
    void followsTheExplicitCreatedRunningReviewPath() {
        var created = ResumeTask.created("task_01", "res_01", 3, CREATED_AT);
        var running = created.start(STARTED_AT);
        var review = running.requireReview("LOCAL_FAKE_READY_FOR_REVIEW", REVIEW_AT);

        assertThat(created.status()).isEqualTo(TaskStatus.CREATED);
        assertThat(running.status()).isEqualTo(TaskStatus.RUNNING);
        assertThat(review.status()).isEqualTo(TaskStatus.REVIEW_REQUIRED);
        assertThat(review.workflowSummary()).isEqualTo("LOCAL_FAKE_READY_FOR_REVIEW");
        assertThat(review.updatedAt()).isEqualTo(REVIEW_AT);
    }

    @Test
    void rejectsTransitionsFromAReviewState() {
        var review = ResumeTask.created("task_01", "res_01", 3, CREATED_AT)
                .start(STARTED_AT)
                .requireReview("ready", REVIEW_AT);

        assertThatIllegalStateException().isThrownBy(() -> review.start(REVIEW_AT));
        assertThatIllegalStateException().isThrownBy(() -> review.fail("LATE_FAILURE", REVIEW_AT));
    }

    @Test
    void completesOnlyAfterTheTaskReachedHumanReview() {
        var review = ResumeTask.created("task_01", "res_01", 3, CREATED_AT)
                .start(STARTED_AT)
                .requireReview("ready", REVIEW_AT);
        var completedAt = Instant.parse("2026-07-13T00:00:03Z");

        var completed = review.complete(completedAt);

        assertThat(completed.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completed.updatedAt()).isEqualTo(completedAt);
        assertThatIllegalStateException()
                .isThrownBy(() -> completed.complete(completedAt));
        assertThatIllegalStateException()
                .isThrownBy(() -> ResumeTask.created(
                        "task_02", "res_01", 3, CREATED_AT).complete(completedAt));
    }
}
