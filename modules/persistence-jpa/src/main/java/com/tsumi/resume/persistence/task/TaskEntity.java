package com.tsumi.resume.persistence.task;

import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.task.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "resume_task")
public class TaskEntity {

    @Id
    @Column(name = "task_id", nullable = false, length = 80)
    private String taskId;
    @Column(name = "resume_id", nullable = false, length = 80)
    private String resumeId;
    @Column(name = "base_version", nullable = false)
    private long baseVersion;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TaskStatus status;
    @Column(name = "workflow_summary", length = 512)
    private String workflowSummary;
    @Column(name = "failure_code", length = 80)
    private String failureCode;
    @Column(name = "failure_retryable", nullable = false)
    private boolean failureRetryable;
    @Column(name = "attempt", nullable = false)
    private int attempt;
    @Column(name = "repair_count", nullable = false)
    private int repairCount;
    @Column(name = "trace_id", nullable = false, length = 80)
    private String traceId;
    @Column(name = "lease_owner", length = 120)
    private String leaseOwner;
    @Column(name = "lease_until")
    private Instant leaseUntil;
    @Column(name = "domain_revision", nullable = false)
    private long domainRevision;
    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskEntity() {}

    TaskEntity(ResumeTask task) {
        this.taskId = task.taskId();
        update(task);
    }

    void update(ResumeTask task) {
        resumeId = task.resumeId();
        baseVersion = task.baseVersion();
        status = task.status();
        workflowSummary = task.workflowSummary();
        failureCode = task.failureCode();
        failureRetryable = task.failureRetryable();
        attempt = task.attempt();
        repairCount = task.repairCount();
        traceId = task.traceId();
        leaseOwner = task.leaseOwner();
        leaseUntil = task.leaseUntil();
        domainRevision = task.revision();
        createdAt = task.createdAt();
        updatedAt = task.updatedAt();
    }

    long domainRevision() {
        return domainRevision;
    }

    ResumeTask toDomain() {
        return new ResumeTask(
                taskId, resumeId, baseVersion, status, workflowSummary,
                failureCode, failureRetryable, attempt, repairCount,
                traceId, leaseOwner, leaseUntil, domainRevision,
                createdAt, updatedAt);
    }
}
