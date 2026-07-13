package com.tsumi.resume.workflow.review;

import com.tsumi.resume.domain.patch.ResumePatch;
import java.util.List;
import java.util.Optional;

public interface PatchStore {

    ResumePatch save(ResumePatch patch);

    Optional<ResumePatch> find(String taskId, String patchId);

    List<ResumePatch> findByTaskId(String taskId);
}
