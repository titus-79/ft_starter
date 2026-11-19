package com.ftstarter.infrastructure.n8n;

import java.util.List;

public record N8nAnalysisRequest(String uploadId, List<N8nDocumentPayload> documents) {
    public N8nAnalysisRequest {
        documents = List.copyOf(documents);
    }
}
