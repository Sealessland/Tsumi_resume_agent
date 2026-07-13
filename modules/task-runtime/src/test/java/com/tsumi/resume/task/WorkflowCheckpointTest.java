package com.tsumi.resume.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class WorkflowCheckpointTest {

    @Test
    void recordsOnlyArtifactHashesAndSafeFailureCodes() {
        var checkpoint = WorkflowCheckpoint.completed(
                "task_01",
                WorkflowNode.EVIDENCE_GUARD,
                1,
                "sha256:input",
                "sha256:output",
                Instant.parse("2026-07-13T00:00:00Z"));

        assertThat(checkpoint.status()).isEqualTo(CheckpointStatus.COMPLETED);
        assertThat(checkpoint.inputHash()).isEqualTo("sha256:input");
        assertThat(checkpoint.outputHash()).isEqualTo("sha256:output");
        assertThat(checkpoint.errorCode()).isNull();
    }

    @Test
    void failedCheckpointRequiresAStableErrorCode() {
        assertThatThrownBy(() -> WorkflowCheckpoint.failed(
                "task_01",
                WorkflowNode.JD_ANALYST,
                1,
                "sha256:input",
                " ",
                Instant.parse("2026-07-13T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
