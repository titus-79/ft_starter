package com.ftstarter.domain.model;

import java.util.List;

public record ApplicantExtractionResponse(
        double confidenceScore,
        List<String> documentsDetected,
        Applicant applicant) {

    public ApplicantExtractionResponse {
        documentsDetected = documentsDetected == null
                ? List.of()
                : List.copyOf(documentsDetected);
        applicant = applicant == null ? Applicant.empty() : applicant;
    }
}
