package com.tsumi.resume.persistence.checkpoint;

import com.tsumi.resume.task.WorkflowCheckpoint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "workflow_checkpoint")
public class WorkflowCheckpointEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "checkpoint_id") private Long id;
    @Column(name = "task_id", nullable = false, length = 80) private String taskId;
    @Column(name = "node_name", nullable = false, length = 48) private String nodeName;
    @Column(name = "attempt", nullable = false) private int attempt;
    @Column(name = "checkpoint_status", nullable = false, length = 24) private String status;
    @Column(name = "input_hash", length = 96) private String inputHash;
    @Column(name = "output_hash", length = 96) private String outputHash;
    @Column(name = "error_code", length = 80) private String errorCode;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected WorkflowCheckpointEntity() {}
    public WorkflowCheckpointEntity(WorkflowCheckpoint checkpoint) {
        taskId = checkpoint.taskId(); nodeName = checkpoint.node().name();
        attempt = checkpoint.attempt(); status = checkpoint.status().name();
        inputHash = checkpoint.inputHash(); outputHash = checkpoint.outputHash();
        errorCode = checkpoint.errorCode(); updatedAt = checkpoint.updatedAt();
    }
    public String taskId() { return taskId; }
    public String nodeName() { return nodeName; }
    public int attempt() { return attempt; }
    public String status() { return status; }
    public String inputHash() { return inputHash; }
    public String outputHash() { return outputHash; }
    public String errorCode() { return errorCode; }
    public Instant updatedAt() { return updatedAt; }
}
