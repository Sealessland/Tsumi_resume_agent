package com.tsumi.resume.workflow.review;

import java.util.List;

public interface CoverageGapStore {

    void replace(String taskId, List<CoverageGap> gaps);

    List<CoverageGap> findByTaskId(String taskId);
}
