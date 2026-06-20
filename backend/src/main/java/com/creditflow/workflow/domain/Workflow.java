package com.creditflow.workflow.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A single credit-analysis run over one or more uploaded documents.
 * The aggregate root of the system of record.
 */
@Entity
@Table(name = "workflows")
public class Workflow {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkflowStatus status;

    @Column(name = "borrower_name", length = 512)
    private String borrowerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", length = 16)
    private RiskCategory riskCategory;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Workflow() {
        // for JPA
    }

    public static Workflow createNew() {
        Workflow w = new Workflow();
        w.id = UUID.randomUUID();
        w.status = WorkflowStatus.PENDING;
        Instant now = Instant.now();
        w.createdAt = now;
        w.updatedAt = now;
        return w;
    }

    public void markProcessing() {
        this.status = WorkflowStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void complete(WorkflowStatus terminalStatus, String borrowerName, RiskCategory riskCategory) {
        this.status = terminalStatus;
        this.borrowerName = borrowerName;
        this.riskCategory = riskCategory;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }

    public void fail(String errorMessage) {
        this.status = WorkflowStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public WorkflowStatus getStatus() { return status; }
    public String getBorrowerName() { return borrowerName; }
    public RiskCategory getRiskCategory() { return riskCategory; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
