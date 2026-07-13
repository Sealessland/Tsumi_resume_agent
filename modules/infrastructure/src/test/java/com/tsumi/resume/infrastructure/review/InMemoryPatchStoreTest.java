package com.tsumi.resume.infrastructure.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.tsumi.resume.domain.patch.PatchIntent;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.patch.PolicyDecision;
import com.tsumi.resume.domain.patch.ResumePatch;
import com.tsumi.resume.domain.patch.ReviewStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryPatchStoreTest {

    @Test
    void upsertsReviewDecisionAndListsOnlyTaskPatches() {
        var store = new InMemoryPatchStore();
        var pending = patch("task_01", "rp_01");
        store.save(pending);
        store.save(patch("task_02", "rp_02"));
        store.save(pending.reviewedAs(ReviewStatus.ACCEPTED));

        assertThat(store.find("task_01", "rp_01").orElseThrow().reviewStatus())
                .isEqualTo(ReviewStatus.ACCEPTED);
        assertThat(store.findByTaskId("task_01"))
                .extracting(ResumePatch::patchId)
                .containsExactly("rp_01");
    }

    private ResumePatch patch(String taskId, String patchId) {
        return new ResumePatch(
                patchId, taskId, "res_01", 1, PatchOperation.REPLACE,
                "/profile/title", "Java", "Java Agent Engineer",
                PatchIntent.PARAPHRASE,
                List.of("resume:profile"), List.of(), 1.0, List.of(),
                0.9, List.of(), PolicyDecision.ALLOW, ReviewStatus.PENDING);
    }
}
