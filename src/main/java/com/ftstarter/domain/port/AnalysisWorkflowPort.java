package com.ftstarter.domain.port;

import com.ftstarter.domain.model.AnalysisPayload;
import com.ftstarter.domain.model.ApplicantExtractionResponse;

public interface AnalysisWorkflowPort {

    ApplicantExtractionResponse analyze(AnalysisPayload payload);
}
