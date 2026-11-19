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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AnalyzeDocumentsUseCase {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeDocumentsUseCase.class);

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
                .orElseThrow(() -> {
                    log.warn("Analysis rejected: upload session not found, uploadId={}", uploadId);
                    return new UploadNotFoundException(uploadId);
                });

        log.info(
                "Analysis document preparation started: uploadId={}, documentCount={}",
                uploadId,
                session.documents().size());
        List<AnalysisDocument> documents = new ArrayList<>(session.documents().size());
        for (int index = 0; index < session.documents().size(); index++) {
            documents.add(toAnalysisDocument(uploadId, index + 1, session.documents().get(index)));
        }
        log.info(
                "Analysis document preparation completed: uploadId={}, documentCount={}",
                uploadId,
                documents.size());

        log.info("Analysis workflow invocation started: uploadId={}", uploadId);
        ApplicantExtractionResponse result = workflow.analyze(
                new AnalysisPayload(uploadId, documents));
        log.info(
                "Analysis workflow invocation completed: uploadId={}, detectedDocumentCount={}",
                uploadId,
                result.documentsDetected().size());
        ApplicantExtractionResponse savedResult = resultRepository.save(uploadId, result);
        log.info("Analysis result persistence completed: uploadId={}", uploadId);
        return savedResult;
    }

    private AnalysisDocument toAnalysisDocument(
            String uploadId,
            int documentIndex,
            DocumentMetadata document) {
        log.debug(
                "Reading stored document for analysis: uploadId={}, documentIndex={}, sizeBytes={}",
                uploadId,
                documentIndex,
                document.sizeBytes());
        try {
            String base64 = Base64.getEncoder().encodeToString(storage.read(document));
            log.debug(
                    "Stored document prepared for analysis: uploadId={}, documentIndex={}, encodedSizeBytes={}",
                    uploadId,
                    documentIndex,
                    base64.length());
            return new AnalysisDocument(document.name(), document.mimeType(), base64);
        } catch (IOException exception) {
            log.error(
                    "Stored document read failed: uploadId={}, documentIndex={}, exceptionType={}",
                    uploadId,
                    documentIndex,
                    exception.getClass().getSimpleName());
            throw new StorageException(
                    "Could not read uploaded document " + document.name(),
                    exception);
        }
    }
}
