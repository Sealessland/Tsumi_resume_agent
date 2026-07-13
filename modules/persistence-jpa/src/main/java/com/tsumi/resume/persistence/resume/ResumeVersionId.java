package com.tsumi.resume.persistence.resume;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record ResumeVersionId(
        @Column(name = "resume_id", length = 80) String resumeId,
        @Column(name = "version_no") long version) implements Serializable {

    protected ResumeVersionId() { this(null, 0); }
}
