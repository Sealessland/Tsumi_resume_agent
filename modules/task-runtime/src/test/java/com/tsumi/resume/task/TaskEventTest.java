package com.tsumi.resume.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaskEventTest {

    @Test
    void defensivelyCopiesSafeStructuredMetadata() {
        var data = new LinkedHashMap<String, String>();
        data.put("node", "evidence-guard");
        var event = new TaskEvent(
                42,
                "task_01",
                "agent.node.completed",
                TaskStatus.VERIFYING,
                1,
                "trace_01",
                Instant.parse("2026-07-13T00:00:00Z"),
                data);

        data.put("prompt", "secret prompt");

        assertThat(event.data()).containsExactlyEntriesOf(
                Map.of("node", "evidence-guard"));
        assertThatThrownBy(() -> event.data().put("rawResume", "PII"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsInvalidEventIdentity() {
        assertThatThrownBy(() -> new TaskEvent(
                0, "task_01", "task.created", TaskStatus.CREATED, 1,
                "trace_01", Instant.parse("2026-07-13T00:00:00Z"), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
