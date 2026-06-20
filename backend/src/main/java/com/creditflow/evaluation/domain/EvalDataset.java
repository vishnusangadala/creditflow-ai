package com.creditflow.evaluation.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A named collection of golden eval cases. */
@Entity
@Table(name = "eval_datasets")
public class EvalDataset {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EvalDataset() {
        // for JPA
    }

    public static EvalDataset create(String name, String description) {
        EvalDataset d = new EvalDataset();
        d.id = UUID.randomUUID();
        d.name = name;
        d.description = description;
        d.createdAt = Instant.now();
        return d;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
