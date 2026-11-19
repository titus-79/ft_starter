package com.ftstarter.domain.port;

import com.ftstarter.domain.model.ApplicantExtractionResponse;
import java.util.Optional;

public interface AnalysisResultRepository {

    ApplicantExtractionResponse save(String uploadId, ApplicantExtractionResponse result);

    Optional<ApplicantExtractionResponse> findByUploadId(String uploadId);
}
