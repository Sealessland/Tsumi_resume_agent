package com.tsumi.resume.persistence.workflow;

import com.tsumi.resume.workflow.WorkflowRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "workflow_request")
public class WorkflowRequestEntity {
    @Id
    @Column(name = "task_id", nullable = false, length = 80)
    private String taskId;
    @Column(name = "job_description", nullable = false, columnDefinition = "text")
    private String jobDescription;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkflowRequestEntity() {}

    WorkflowRequestEntity(WorkflowRequest request) {
        taskId = request.taskId();
        jobDescription = request.jobDescription();
        createdAt = request.createdAt();
    }

    WorkflowRequest toDomain() {
        return new WorkflowRequest(taskId, jobDescription, createdAt);
    }
}
