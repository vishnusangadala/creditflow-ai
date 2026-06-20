package com.creditflow.document.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditflow.document.domain.Document;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByWorkflowIdOrderByCreatedAt(UUID workflowId);
}
