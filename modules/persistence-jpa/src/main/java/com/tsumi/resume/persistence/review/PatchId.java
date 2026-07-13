package com.tsumi.resume.persistence.review;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record PatchId(
        @Column(name = "task_id", length = 80) String taskId,
        @Column(name = "patch_id", length = 80) String patchId) implements Serializable {
    protected PatchId() { this(null, null); }
}
