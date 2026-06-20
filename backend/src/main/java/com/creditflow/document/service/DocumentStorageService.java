package com.creditflow.document.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.creditflow.config.CreditFlowProperties;

/**
 * Persists uploaded files to a local directory and reads them back.
 *
 * <p>Local-disk storage is the right level of complexity for Phase 1. Swapping in
 * S3/GCS later is a single-class change because callers depend only on the
 * returned {@code storagePath} string. Files are namespaced per workflow.
 */
@Service
public class DocumentStorageService {

    private final Path uploadRoot;

    public DocumentStorageService(CreditFlowProperties properties) {
        this.uploadRoot = Path.of(properties.storage().uploadDir());
    }

    public StoredFile store(UUID workflowId, MultipartFile file) {
        try {
            Path workflowDir = uploadRoot.resolve(workflowId.toString());
            Files.createDirectories(workflowDir);

            String safeName = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
            Path target = workflowDir.resolve(safeName);
            file.transferTo(target.toAbsolutePath());

            return new StoredFile(target.toString(), file.getSize());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
    }

    public byte[] read(String storagePath) {
        try {
            return Files.readAllBytes(Path.of(storagePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read stored file: " + storagePath, e);
        }
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document.pdf";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record StoredFile(String storagePath, long sizeBytes) {}
}
