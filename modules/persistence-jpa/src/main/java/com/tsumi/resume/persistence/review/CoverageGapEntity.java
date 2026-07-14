package com.tsumi.resume.persistence.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_coverage_gap")
public class CoverageGapEntity {
    @Id
    @Column(name = "gap_id", nullable = false, length = 200)
    private String gapId;

    @Column(name = "task_id", nullable = false, length = 80)
    private String taskId;

    @Column(name = "ordinal_no", nullable = false)
    private int ordinalNo;

    @Column(name = "gap_json", nullable = false, columnDefinition = "text")
    private String gapJson;

    protected CoverageGapEntity() {}

    public CoverageGapEntity(String taskId, int ordinalNo, String gapJson) {
        this.gapId = taskId + ":" + ordinalNo;
        this.taskId = taskId;
        this.ordinalNo = ordinalNo;
        this.gapJson = gapJson;
    }

    public String gapJson() {
        return gapJson;
    }
}
