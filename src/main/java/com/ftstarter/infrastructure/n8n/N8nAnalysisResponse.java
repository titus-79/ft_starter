package com.ftstarter.infrastructure.n8n;

import com.ftstarter.domain.model.Applicant;
import java.util.List;

public record N8nAnalysisResponse(
        Double confidenceScore,
        List<String> documentsDetected,
        Applicant applicant) {}
