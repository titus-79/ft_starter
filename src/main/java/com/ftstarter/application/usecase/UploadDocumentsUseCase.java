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
import org.springframework.stereotype.Service;

@Service
public class UploadDocumentsUseCase {

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
        validateFiles(files);
        String uploadId = uploadIdGenerator.generate();
        List<DocumentMetadata> storedDocuments = new ArrayList<>();

        try {
            for (IncomingDocument file : files) {
                String extension = extensionOf(file.originalName());
                String mimeType = mimeTypeFor(extension);
                try (InputStream content = file.content().open()) {
                    storedDocuments.add(storage.store(
                            uploadId,
                            file.originalName(),
                            mimeType,
                            file.sizeBytes(),
                            content));
                }
            }
        } catch (IOException exception) {
            try {
                storage.deleteUpload(uploadId);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new StorageException("Could not store uploaded documents", exception);
        }

        UploadSession session = new UploadSession(
                uploadId,
                UploadStatus.UPLOADED,
                storedDocuments,
                Instant.now(clock));
        return sessionRepository.save(session);
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
