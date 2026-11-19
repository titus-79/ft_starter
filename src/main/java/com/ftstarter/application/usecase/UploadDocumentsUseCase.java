package com.ftstarter.application.usecase;

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
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UploadDocumentsUseCase {

    private static final Logger log = LoggerFactory.getLogger(UploadDocumentsUseCase.class);

    private final UploadSessionRepository sessionRepository;
    private final DocumentStoragePort storage;
    private final UploadIdGenerator uploadIdGenerator;
    private final AppProperties.Upload uploadProperties;
    private final Clock clock;

    public UploadDocumentsUseCase(
            UploadSessionRepository sessionRepository,
            DocumentStoragePort storage,
            UploadIdGenerator uploadIdGenerator,
            AppProperties properties,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.storage = storage;
        this.uploadIdGenerator = uploadIdGenerator;
        this.uploadProperties = properties.upload();
        this.clock = clock;
    }

    public UploadSession execute(List<IncomingDocument> files) {
        int documentCount = files == null ? 0 : files.size();
        long totalSizeBytes = files == null
                ? 0
                : files.stream()
                        .filter(file -> file != null)
                        .mapToLong(IncomingDocument::sizeBytes)
                        .sum();
        log.info(
                "Document validation started: documentCount={}, totalSizeBytes={}",
                documentCount,
                totalSizeBytes);
        try {
            validateFiles(files);
        } catch (RuntimeException exception) {
            log.warn(
                    "Document validation failed: documentCount={}, exceptionType={}",
                    documentCount,
                    exception.getClass().getSimpleName());
            throw exception;
        }
        log.info("Document validation completed: documentCount={}", documentCount);

        String uploadId = uploadIdGenerator.generate();
        log.info("Upload session allocated: uploadId={}", uploadId);
        List<DocumentMetadata> storedDocuments = new ArrayList<>();

        try {
            int documentIndex = 0;
            for (IncomingDocument file : files) {
                documentIndex++;
                String extension = extensionOf(file.originalName());
                String mimeType = mimeTypeFor(extension);
                log.info(
                        "Document storage started: uploadId={}, documentIndex={}, documentCount={}, sizeBytes={}, mimeType={}",
                        uploadId,
                        documentIndex,
                        documentCount,
                        file.sizeBytes(),
                        mimeType);
                try (InputStream content = file.content().open()) {
                    storedDocuments.add(storage.store(
                            uploadId,
                            file.originalName(),
                            mimeType,
                            file.sizeBytes(),
                            content));
                }
                log.info(
                        "Document storage completed: uploadId={}, documentIndex={}, documentCount={}",
                        uploadId,
                        documentIndex,
                        documentCount);
            }
        } catch (IOException exception) {
            log.error(
                    "Document storage failed: uploadId={}, storedDocumentCount={}, exceptionType={}",
                    uploadId,
                    storedDocuments.size(),
                    exception.getClass().getSimpleName());
            try {
                storage.deleteUpload(uploadId);
                log.info("Partial upload cleanup completed: uploadId={}", uploadId);
            } catch (IOException cleanupException) {
                log.error(
                        "Partial upload cleanup failed: uploadId={}, exceptionType={}",
                        uploadId,
                        cleanupException.getClass().getSimpleName());
                exception.addSuppressed(cleanupException);
            }
            throw new StorageException("Could not store uploaded documents", exception);
        }

        UploadSession session = new UploadSession(
                uploadId,
                UploadStatus.UPLOADED,
                storedDocuments,
                Instant.now(clock));
        log.info(
                "Upload session persistence started: uploadId={}, documentCount={}",
                uploadId,
                storedDocuments.size());
        try {
            UploadSession savedSession = sessionRepository.save(session);
            log.info(
                    "Upload session persistence completed: uploadId={}, documentCount={}, status={}",
                    savedSession.uploadId(),
                    savedSession.documents().size(),
                    savedSession.status());
            return savedSession;
        } catch (RuntimeException exception) {
            log.error(
                    "Upload session persistence failed: uploadId={}, exceptionType={}",
                    uploadId,
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private void validateFiles(List<IncomingDocument> files) {
        if (files == null || files.isEmpty()) {
            throw new InvalidDocumentException("At least one document is required");
        }
        if (files.size() > uploadProperties.maxFiles()) {
            throw new TooManyDocumentsException(uploadProperties.maxFiles());
        }

        for (IncomingDocument file : files) {
            if (file == null || file.originalName() == null
                    || file.originalName().isBlank() || file.content() == null
                    || file.sizeBytes() <= 0) {
                throw new InvalidDocumentException("A document is empty or invalid");
            }

            String extension = extensionOf(file.originalName());
            if (!uploadProperties.allowedExtensions().contains(extension)) {
                throw new UnsupportedFileExtensionException(file.originalName());
            }
            if (file.sizeBytes() > uploadProperties.maxFileSize().toBytes()) {
                throw new DocumentTooLargeException(
                        file.originalName(),
                        uploadProperties.maxFileSize().toBytes());
            }
        }
    }

    private String extensionOf(String filename) {
        String basename = filename.replace('\\', '/');
        basename = basename.substring(basename.lastIndexOf('/') + 1);
        int dot = basename.lastIndexOf('.');
        if (dot < 0 || dot == basename.length() - 1) return "";
        return basename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String mimeTypeFor(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> throw new UnsupportedFileExtensionException(extension);
        };
    }
}
