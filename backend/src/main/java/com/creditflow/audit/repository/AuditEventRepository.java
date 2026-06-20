package com.creditflow.audit.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditflow.audit.domain.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByWorkflowIdOrderByCreatedAt(UUID workflowId);

    List<AuditEvent> findTop100ByOrderByCreatedAtDesc();
}
