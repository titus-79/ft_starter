package com.ftstarter.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ftstarter.domain.model.AnalysisPayload;
import com.ftstarter.domain.model.Applicant;
import com.ftstarter.domain.model.ApplicantExtractionResponse;
import com.ftstarter.domain.model.DocumentMetadata;
import com.ftstarter.domain.model.UploadSession;
import com.ftstarter.domain.model.UploadStatus;
import com.ftstarter.domain.port.AnalysisResultRepository;
import com.ftstarter.domain.port.AnalysisWorkflowPort;
import com.ftstarter.domain.port.DocumentStoragePort;
import com.ftstarter.domain.port.UploadSessionRepository;
import com.ftstarter.shared.exception.UploadNotFoundException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyzeDocumentsUseCaseTest {

    @Mock
    private UploadSessionRepository sessionRepository;

    @Mock
    private AnalysisResultRepository resultRepository;

    @Mock
    private DocumentStoragePort storage;

    @Mock
    private AnalysisWorkflowPort workflow;

    @InjectMocks
    private AnalyzeDocumentsUseCase useCase;

    @Test
    void encodesFilesAndStoresTheNormalizedWorkflowResponse() throws Exception {
        DocumentMetadata document = new DocumentMetadata(
                "cni.pdf", "application/pdf", "UPL-2026-000000000001/cni.pdf", 3);
        UploadSession session = new UploadSession(
                "UPL-2026-000000000001",
                UploadStatus.UPLOADED,
                List.of(document),
                Instant.now());
        ApplicantExtractionResponse response =
                new ApplicantExtractionResponse(0.9, List.of("CNI"), Applicant.empty());
        when(sessionRepository.findById(session.uploadId())).thenReturn(Optional.of(session));
        when(storage.read(document)).thenReturn(new byte[] {1, 2, 3});
        when(workflow.analyze(any(AnalysisPayload.class))).thenReturn(response);
        when(resultRepository.save(session.uploadId(), response)).thenReturn(response);

        ApplicantExtractionResponse result = useCase.execute(session.uploadId());

        assertEquals(response, result);
        ArgumentCaptor<AnalysisPayload> payload = ArgumentCaptor.forClass(AnalysisPayload.class);
        verify(workflow).analyze(payload.capture());
        assertEquals(
                Base64.getEncoder().encodeToString(new byte[] {1, 2, 3}),
                payload.getValue().documents().getFirst().base64());
        verify(resultRepository).save(session.uploadId(), response);
    }

    @Test
    void rejectsUnknownUploadIdsWithoutCallingTheWorkflow() {
        when(sessionRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UploadNotFoundException.class, () -> useCase.execute("missing"));

        verify(workflow, never()).analyze(any());
        verify(resultRepository, never()).save(any(), any());
    }
}
