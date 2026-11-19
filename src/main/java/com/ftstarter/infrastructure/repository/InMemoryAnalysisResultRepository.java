package com.ftstarter.infrastructure.repository;

import com.ftstarter.domain.model.ApplicantExtractionResponse;
import com.ftstarter.domain.port.AnalysisResultRepository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryAnalysisResultRepository implements AnalysisResultRepository {

    private final ConcurrentHashMap<String, ApplicantExtractionResponse> results =
            new ConcurrentHashMap<>();

    @Override
    public ApplicantExtractionResponse save(
            String uploadId,
            ApplicantExtractionResponse result) {
        results.put(uploadId, result);
        return result;
    }

    @Override
    public Optional<ApplicantExtractionResponse> findByUploadId(String uploadId) {
        return Optional.ofNullable(results.get(uploadId));
    }
}
