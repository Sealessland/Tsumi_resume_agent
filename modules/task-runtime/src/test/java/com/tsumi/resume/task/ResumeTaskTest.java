package com.tsumi.resume.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ResumeTaskTest {

    private static final Instant T0 = Instant.parse("2026-07-13T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-07-13T00:00:01Z");

    @Test
    void followsTheExplicitAgentAndHumanReviewPath() {
        var created = task();
        var analyzing = created.analyze(T1);
        var proposing = analyzing.propose(T1);
        var verifying = proposing.verify(T1);
        var reviewReady = verifying.reviewReady("SAFE_PATCHES_READY", T1);
        var approved = reviewReady.approve(T1);
        var completed = approved.complete(T1);

        assertThat(created.status()).isEqualTo(TaskStatus.CREATED);
        assertThat(analyzing.status()).isEqualTo(TaskStatus.ANALYZING);
        assertThat(proposing.status()).isEqualTo(TaskStatus.PROPOSING);
        assertThat(verifying.status()).isEqualTo(TaskStatus.VERIFYING);
        assertThat(reviewReady.status()).isEqualTo(TaskStatus.REVIEW_READY);
        assertThat(approved.status()).isEqualTo(TaskStatus.APPROVED);
        assertThat(completed.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completed.revision()).isEqualTo(6);
        assertThat(completed.traceId()).isEqualTo("trace_01");
    }

    @Test
    void permitsOnlyOneSemanticRepairFromVerification() {
        var verifying = task().analyze(T1).propose(T1).verify(T1);

        var repaired = verifying.repair(T1);

        assertThat(repaired.status()).isEqualTo(TaskStatus.PROPOSING);
        assertThat(repaired.repairCount()).isEqualTo(1);
        var verifyingAgain = repaired.verify(T1);
        assertThatIllegalStateException().isThrownBy(() -> verifyingAgain.repair(T1));
    }

    @Test
    void retriesOnlyRetryableFailuresAndResetsSemanticRepair() {
        var failed = task().analyze(T1).propose(T1).verify(T1).repair(T1)
                .fail("MODEL_TIMEOUT", true, T1);

        var retried = failed.retry(T1);

        assertThat(retried.status()).isEqualTo(TaskStatus.ANALYZING);
        assertThat(retried.attempt()).isEqualTo(2);
        assertThat(retried.repairCount()).isZero();
        assertThat(retried.failureCode()).isNull();
        assertThat(retried.failureRetryable()).isFalse();

        var permanentFailure = task().analyze(T1)
                .fail("MODEL_OUTPUT_REJECTED", false, T1);
        assertThatIllegalStateException().isThrownBy(() -> permanentFailure.retry(T1));
    }

    @Test
    void cancellationIsTerminalAndBlocksLaterWork() {
        var cancelled = task().analyze(T1).propose(T1).cancel(T1);

        assertThat(cancelled.status()).isEqualTo(TaskStatus.CANCELLED);
        assertThatIllegalStateException().isThrownBy(() -> cancelled.verify(T1));
        assertThatIllegalStateException().isThrownBy(() -> cancelled.retry(T1));
        assertThatIllegalStateException().isThrownBy(() -> cancelled.complete(T1));
    }

    @Test
    void leasesOnlyActiveExecutionStatesAndCanBeReleased() {
        var leased = task().lease(
                "instance_01", Instant.parse("2026-07-13T00:01:00Z"), T1);

        assertThat(leased.leaseOwner()).isEqualTo("instance_01");
        assertThat(leased.leaseUntil()).isEqualTo(
                Instant.parse("2026-07-13T00:01:00Z"));
        var released = leased.releaseLease(T1);
        assertThat(released.leaseOwner()).isNull();
        assertThat(released.leaseUntil()).isNull();

        var completed = task().analyze(T1).propose(T1).verify(T1)
                .reviewReady("ready", T1).approve(T1).complete(T1);
        assertThatIllegalStateException().isThrownBy(() -> completed.lease(
                "instance_01", Instant.parse("2026-07-13T00:01:00Z"), T1));
    }

    @Test
    void rejectsSkippedOrRepeatedTransitions() {
        assertThatIllegalStateException().isThrownBy(() -> task().propose(T1));
        assertThatIllegalStateException().isThrownBy(() -> task().complete(T1));

        var reviewReady = task().analyze(T1).propose(T1).verify(T1)
                .reviewReady("ready", T1);
        assertThatIllegalStateException().isThrownBy(() -> reviewReady.verify(T1));
        assertThatIllegalStateException().isThrownBy(() -> reviewReady.complete(T1));
    }

    private ResumeTask task() {
        return ResumeTask.created(
                "task_01", "res_01", 3, "trace_01", T0);
    }
}
