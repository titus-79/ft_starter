package com.ftstarter.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ftstarter.application.service.UploadIdGenerator;
import com.ftstarter.config.AppProperties;
import com.ftstarter.domain.model.DocumentMetadata;
import com.ftstarter.domain.model.UploadSession;
import com.ftstarter.domain.model.UploadStatus;
import com.ftstarter.domain.port.DocumentStoragePort;
import com.ftstarter.domain.port.UploadSessionRepository;
import com.ftstarter.shared.exception.DocumentTooLargeException;
import com.ftstarter.shared.exception.InvalidDocumentException;
import com.ftstarter.shared.exception.StorageException;
import com.ftstarter.shared.exception.TooManyDocumentsException;
import com.ftstarter.shared.exception.UnsupportedFileExtensionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.util.unit.DataSize;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UploadDocumentsUseCaseTest {

    @Mock
    private UploadSessionRepository sessionRepository;

    @Mock
    private DocumentStoragePort storage;

    @Mock
    private UploadIdGenerator uploadIdGenerator;

    private UploadDocumentsUseCase useCase;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(
                new AppProperties.Storage("uploads"),
                new AppProperties.Upload(
                        DataSize.ofMegabytes(20),
                        20,
                        List.of("pdf", "png", "jpg", "jpeg")),
                null,
                new AppProperties.Cors(List.of()));
        useCase = new UploadDocumentsUseCase(
                sessionRepository,
                storage,
                uploadIdGenerator,
                properties,
                Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneOffset.UTC));
        org.mockito.Mockito.lenient()
                .when(uploadIdGenerator.generate())
                .thenReturn("UPL-2026-000000000001");
    }

    @Test
    void storesFilesAndCreatesAnInMemorySession() throws Exception {
        when(storage.store(
                eq("UPL-2026-000000000001"),
                eq("cni.pdf"),
                eq("application/pdf"),
                eq(4L),
                any()))
                .thenReturn(new DocumentMetadata(
                        "cni.pdf",
                        "application/pdf",
                        "UPL-2026-000000000001/cni.pdf",
                        4));
        when(sessionRepository.save(any(UploadSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UploadSession session = useCase.execute(List.of(document("cni.pdf", 4)));

        assertEquals("UPL-2026-000000000001", session.uploadId());
        assertEquals(UploadStatus.UPLOADED, session.status());
        assertEquals(1, session.documents().size());
        assertEquals(Instant.parse("2026-11-19T12:00:00Z"), session.createdAt());
        verify(sessionRepository).save(session);
    }

    @Test
    void rejectsUnsupportedExtensionsBeforeWriting() throws Exception {
        assertThrows(
                UnsupportedFileExtensionException.class,
                () -> useCase.execute(List.of(document("identity.docx", 4))));
        verify(storage, never()).store(any(), any(), any(), any(Long.class), any());
    }

    @Test
    void rejectsFilesAboveConfiguredLimit() throws Exception {
        assertThrows(
                DocumentTooLargeException.class,
                () -> useCase.execute(List.of(document("large.pdf", 20 * 1024 * 1024 + 1L))));
        verify(storage, never()).store(any(), any(), any(), any(Long.class), any());
    }

    @Test
    void rejectsEmptyUploadsAndMoreThanTwentyFiles() throws Exception {
        assertThrows(InvalidDocumentException.class, () -> useCase.execute(List.of()));
        List<IncomingDocument> files = java.util.stream.IntStream.range(0, 21)
                .mapToObj(index -> document(index + ".pdf", 1))
                .toList();
        assertThrows(TooManyDocumentsException.class, () -> useCase.execute(files));
        verify(storage, never()).store(any(), any(), any(), any(Long.class), any());
    }

    @Test
    void cleansTheUploadDirectoryWhenAWriteFails() throws Exception {
        when(storage.store(
                eq("UPL-2026-000000000001"),
                eq("cni.pdf"),
                eq("application/pdf"),
                eq(4L),
                any())).thenThrow(new IOException("disk failure"));

        assertThrows(
                StorageException.class,
                () -> useCase.execute(List.of(document("cni.pdf", 4))));

        verify(storage).deleteUpload("UPL-2026-000000000001");
        verify(sessionRepository, never()).save(any());
    }

    private IncomingDocument document(String name, long size) {
        return new IncomingDocument(
                name,
                size,
                () -> new ByteArrayInputStream(new byte[] {1, 2, 3, 4}));
    }
}
