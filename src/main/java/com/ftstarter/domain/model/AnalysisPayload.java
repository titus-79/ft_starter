package com.ftstarter.domain.model;

import java.util.List;

public record AnalysisPayload(String uploadId, List<AnalysisDocument> documents) {

    public AnalysisPayload {
        documents = List.copyOf(documents);
    }
}
