package com.creditflow.audit.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.creditflow.audit.domain.AuditEvent;
import com.creditflow.audit.domain.AuditEventType;
import com.creditflow.audit.repository.AuditEventRepository;
import com.creditflow.common.ActorRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Records audit events. Every Phase 2 action funnels through {@link #record} so
 * the trail is uniform and complete. Writes participate in the caller's
 * transaction — an audit row and the action it describes commit together.
 */
@Service
public class AuditEventService {

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public AuditEventService(AuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void record(UUID workflowId, AuditEventType type, String actor, ActorRole role,
                       String summary, Object payload) {
        repository.save(AuditEvent.create(workflowId, type, actor, role, summary, toJson(payload)));
    }

    public List<AuditEvent> forWorkflow(UUID workflowId) {
        return repository.findByWorkflowIdOrderByCreatedAt(workflowId);
    }

    public List<AuditEvent> recent() {
        return repository.findTop100ByOrderByCreatedAtDesc();
    }

    private String toJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
