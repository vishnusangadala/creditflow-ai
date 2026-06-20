package com.creditflow.audit.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.creditflow.audit.domain.AuditEvent;
import com.creditflow.audit.service.AuditEventService;

/** A global, recent-first feed of the audit trail — the observability surface. */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditEventService auditService;

    public AuditController(AuditEventService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<EventView> recent() {
        return auditService.recent().stream().map(this::toView).toList();
    }

    private EventView toView(AuditEvent e) {
        return new EventView(e.getWorkflowId(), e.getEventType().name(), e.getActor(),
                e.getActorRole().name(), e.getSummary(), e.getCreatedAt());
    }

    public record EventView(UUID workflowId, String eventType, String actor, String actorRole,
                            String summary, Instant createdAt) {}
}
