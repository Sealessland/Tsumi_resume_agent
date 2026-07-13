package com.tsumi.resume.workflow.resume;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VersionedResumeServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registersAndLoadsAnImmutableVersionThroughThePort() {
        var store = new RecordingResumeVersionStore();
        var service = new VersionedResumeService(store);
        var resume = resume("res_01", 1);

        service.register(resume);
        resume.put("version", 99);

        assertThat(service.get("res_01", 1).path("version").asLong()).isEqualTo(1);
        assertThat(service.versions("res_01")).containsExactly(1L);
    }

    @Test
    void rejectsInvalidEnvelopeIdentityBeforeCallingTheStore() {
        var store = new RecordingResumeVersionStore();
        var service = new VersionedResumeService(store);
        var invalid = resume("bad", 0);

        assertThatThrownBy(() -> service.register(invalid))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(store.saved).isEmpty();
    }

    @Test
    void reportsUnknownVersion() {
        var service = new VersionedResumeService(new RecordingResumeVersionStore());

        assertThatThrownBy(() -> service.get("res_missing", 1))
                .isInstanceOf(ResumeVersionNotFoundException.class);
    }

    private ObjectNode resume(String resumeId, long version) {
        var node = objectMapper.createObjectNode();
        node.put("resumeId", resumeId);
        node.put("version", version);
        node.put("schemaVersion", 13);
        return node;
    }

    private static final class RecordingResumeVersionStore implements ResumeVersionStore {
        private final List<ObjectNode> saved = new ArrayList<>();

        @Override
        public ObjectNode save(ObjectNode resume) {
            var copy = resume.deepCopy();
            saved.add(copy);
            return copy.deepCopy();
        }

        @Override
        public Optional<ObjectNode> find(String resumeId, long version) {
            return saved.stream()
                    .filter(item -> item.path("resumeId").asText().equals(resumeId))
                    .filter(item -> item.path("version").asLong() == version)
                    .findFirst()
                    .map(ObjectNode::deepCopy);
        }

        @Override
        public List<Long> versions(String resumeId) {
            return saved.stream()
                    .filter(item -> item.path("resumeId").asText().equals(resumeId))
                    .map(item -> item.path("version").asLong())
                    .sorted()
                    .toList();
        }
    }
}
