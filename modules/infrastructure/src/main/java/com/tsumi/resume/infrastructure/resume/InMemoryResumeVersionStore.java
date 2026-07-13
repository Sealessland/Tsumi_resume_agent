package com.tsumi.resume.infrastructure.resume;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsumi.resume.workflow.resume.DuplicateResumeVersionException;
import com.tsumi.resume.workflow.resume.ResumeVersionStore;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryResumeVersionStore implements ResumeVersionStore {

    private final ConcurrentMap<Key, ObjectNode> versions = new ConcurrentHashMap<>();

    @Override
    public ObjectNode save(ObjectNode resume) {
        var resumeId = resume.path("resumeId").asText();
        var version = resume.path("version").asLong();
        var copy = resume.deepCopy();
        if (versions.putIfAbsent(new Key(resumeId, version), copy) != null) {
            throw new DuplicateResumeVersionException(resumeId, version);
        }
        return copy.deepCopy();
    }

    @Override
    public Optional<ObjectNode> find(String resumeId, long version) {
        return Optional.ofNullable(versions.get(new Key(resumeId, version)))
                .map(ObjectNode::deepCopy);
    }

    @Override
    public List<Long> versions(String resumeId) {
        return versions.keySet().stream()
                .filter(key -> key.resumeId().equals(resumeId))
                .map(Key::version)
                .sorted()
                .toList();
    }

    private record Key(String resumeId, long version) {}
}
