package com.ftstarter.infrastructure.rest.dto;

import com.ftstarter.domain.model.UploadSession;
import com.ftstarter.domain.model.UploadStatus;

public record UploadResponse(String uploadId, UploadStatus status, int documentCount) {

    public static UploadResponse from(UploadSession session) {
        return new UploadResponse(
                session.uploadId(),
                session.status(),
                session.documents().size());
    }
}
