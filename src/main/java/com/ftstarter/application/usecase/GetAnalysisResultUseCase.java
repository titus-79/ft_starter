package com.ftstarter.application.usecase;

import com.ftstarter.domain.model.ApplicantExtractionResponse;
import com.ftstarter.domain.port.AnalysisResultRepository;
import com.ftstarter.shared.exception.AnalysisNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetAnalysisResultUseCase {

    private final AnalysisResultRepository repository;

    public GetAnalysisResultUseCase(AnalysisResultRepository repository) {
        this.repository = repository;
    }

    public ApplicantExtractionResponse execute(String uploadId) {
        return repository.findByUploadId(uploadId)
                .orElseThrow(() -> new AnalysisNotFoundException(uploadId));
    }
}
