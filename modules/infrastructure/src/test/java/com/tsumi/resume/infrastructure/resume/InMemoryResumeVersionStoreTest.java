package com.tsumi.resume.infrastructure.resume;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsumi.resume.workflow.resume.DuplicateResumeVersionException;
import org.junit.jupiter.api.Test;

class InMemoryResumeVersionStoreTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void storesImmutableCopiesAndSortsVersions() {
        var store = new InMemoryResumeVersionStore();
        var versionTwo = resume(2);
        store.save(versionTwo);
        store.save(resume(1));
        versionTwo.put("version", 99);

        var loaded = store.find("res_01", 2).orElseThrow();
        loaded.put("version", 88);

        assertThat(store.find("res_01", 2).orElseThrow().path("version").asLong())
                .isEqualTo(2);
        assertThat(store.versions("res_01")).containsExactly(1L, 2L);
    }

    @Test
    void rejectsDuplicateImmutableVersion() {
        var store = new InMemoryResumeVersionStore();
        store.save(resume(1));

        assertThatThrownBy(() -> store.save(resume(1)))
                .isInstanceOf(DuplicateResumeVersionException.class);
    }

    private com.fasterxml.jackson.databind.node.ObjectNode resume(long version) {
        var node = objectMapper.createObjectNode();
        node.put("resumeId", "res_01");
        node.put("version", version);
        node.put("schemaVersion", 13);
        return node;
    }
}
