package com.ftstarter.application.usecase;

import com.ftstarter.domain.model.AnalysisDocument;
import com.ftstarter.domain.model.AnalysisPayload;
import com.ftstarter.domain.model.ApplicantExtractionResponse;
import com.ftstarter.domain.model.DocumentMetadata;
import com.ftstarter.domain.model.UploadSession;
import com.ftstarter.domain.port.AnalysisResultRepository;
import com.ftstarter.domain.port.AnalysisWorkflowPort;
import com.ftstarter.domain.port.DocumentStoragePort;
import com.ftstarter.domain.port.UploadSessionRepository;
import com.ftstarter.shared.exception.StorageException;
import com.ftstarter.shared.exception.UploadNotFoundException;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AnalyzeDocumentsUseCase {

    private final UploadSessionRepository sessionRepository;
    private final AnalysisResultRepository resultRepository;
    private final DocumentStoragePort storage;
    private final AnalysisWorkflowPort workflow;

    public AnalyzeDocumentsUseCase(
            UploadSessionRepository sessionRepository,
            AnalysisResultRepository resultRepository,
            DocumentStoragePort storage,
            AnalysisWorkflowPort workflow) {
        this.sessionRepository = sessionRepository;
        this.resultRepository = resultRepository;
        this.storage = storage;
        this.workflow = workflow;
    }

    public ApplicantExtractionResponse execute(String uploadId) {
        UploadSession session = sessionRepository.findById(uploadId)
                .orElseThrow(() -> new UploadNotFoundException(uploadId));

        List<AnalysisDocument> documents = session.documents().stream()
                .map(this::toAnalysisDocument)
                .toList();
        ApplicantExtractionResponse result = workflow.analyze(
                new AnalysisPayload(uploadId, documents));
        return resultRepository.save(uploadId, result);
    }

    private AnalysisDocument toAnalysisDocument(DocumentMetadata document) {
        try {
            String base64 = Base64.getEncoder().encodeToString(storage.read(document));
            return new AnalysisDocument(document.name(), document.mimeType(), base64);
        } catch (IOException exception) {
            throw new StorageException(
                    "Could not read uploaded document " + document.name(),
                    exception);
        }
    }
}
