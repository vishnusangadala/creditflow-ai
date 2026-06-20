package com.creditflow.document.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** An uploaded source document belonging to a workflow. */
@Entity
@Table(name = "documents")
public class Document {

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(nullable = false, length = 1024)
    private String filename;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "storage_path", length = 2048)
    private String storagePath;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "extracted_text")
    private String extractedText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Document() {
        // for JPA
    }

    public static Document create(UUID workflowId, String filename, String contentType,
                                  long sizeBytes, String storagePath) {
        Document d = new Document();
        d.id = UUID.randomUUID();
        d.workflowId = workflowId;
        d.filename = filename;
        d.contentType = contentType;
        d.sizeBytes = sizeBytes;
        d.storagePath = storagePath;
        d.createdAt = Instant.now();
        return d;
    }

    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getStoragePath() { return storagePath; }
    public Integer getPageCount() { return pageCount; }
    public String getExtractedText() { return extractedText; }
    public Instant getCreatedAt() { return createdAt; }
}
