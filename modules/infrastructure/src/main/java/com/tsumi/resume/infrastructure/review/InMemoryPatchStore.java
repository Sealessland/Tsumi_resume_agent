package com.tsumi.resume.infrastructure.review;

import com.tsumi.resume.domain.patch.ResumePatch;
import com.tsumi.resume.workflow.review.PatchStore;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryPatchStore implements PatchStore {

    private final ConcurrentMap<Key, ResumePatch> patches = new ConcurrentHashMap<>();

    @Override
    public ResumePatch save(ResumePatch patch) {
        patches.put(new Key(patch.taskId(), patch.patchId()), patch);
        return patch;
    }

    @Override
    public Optional<ResumePatch> find(String taskId, String patchId) {
        return Optional.ofNullable(patches.get(new Key(taskId, patchId)));
    }

    @Override
    public List<ResumePatch> findByTaskId(String taskId) {
        return patches.entrySet().stream()
                .filter(entry -> entry.getKey().taskId().equals(taskId))
                .map(java.util.Map.Entry::getValue)
                .sorted(java.util.Comparator.comparing(ResumePatch::patchId))
                .toList();
    }

    private record Key(String taskId, String patchId) {}
}
