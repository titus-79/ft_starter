package com.ftstarter.domain.model;

import java.time.Instant;
import java.util.List;

public record UploadSession(
        String uploadId,
        UploadStatus status,
        List<DocumentMetadata> documents,
        Instant createdAt) {

    public UploadSession {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }
}
