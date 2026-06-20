package com.creditflow.audit.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.creditflow.common.ActorRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One immutable entry in the audit trail. Append-only by convention — there are
 * no setters and the service never updates or deletes rows. This is the
 * backbone the rest of Phase 2 writes to.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    @Column(name = "workflow_id")
    private UUID workflowId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 48)
    private AuditEventType eventType;

    @Column(nullable = false, length = 255)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false, length = 16)
    private ActorRole actorRole;

    @Column
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
        // for JPA
    }

    public static AuditEvent create(UUID workflowId, AuditEventType eventType, String actor,
                                    ActorRole actorRole, String summary, String payloadJson) {
        AuditEvent e = new AuditEvent();
        e.id = UUID.randomUUID();
        e.workflowId = workflowId;
        e.eventType = eventType;
        e.actor = actor;
        e.actorRole = actorRole;
        e.summary = summary;
        e.payload = payloadJson;
        e.createdAt = Instant.now();
        return e;
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public AuditEventType getEventType() { return eventType; }
    public String getActor() { return actor; }
    public ActorRole getActorRole() { return actorRole; }
    public String getSummary() { return summary; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
}
