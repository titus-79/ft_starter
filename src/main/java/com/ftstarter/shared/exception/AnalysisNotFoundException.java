package com.ftstarter.shared.exception;

import org.springframework.http.HttpStatus;

public class AnalysisNotFoundException extends ApiException {

    public AnalysisNotFoundException(String uploadId) {
        super(
                "ANALYSIS_NOT_FOUND",
                "No analysis found for uploadId",
                HttpStatus.NOT_FOUND,
                java.util.List.of(uploadId),
                null);
    }
}
