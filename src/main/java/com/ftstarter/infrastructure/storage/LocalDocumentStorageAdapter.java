package com.ftstarter.infrastructure.storage;

import com.ftstarter.config.AppProperties;
import com.ftstarter.domain.model.DocumentMetadata;
import com.ftstarter.domain.port.DocumentStoragePort;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LocalDocumentStorageAdapter implements DocumentStoragePort {

    private static final Logger log = LoggerFactory.getLogger(LocalDocumentStorageAdapter.class);
    private static final Pattern UPLOAD_ID_PATTERN =
            Pattern.compile("UPL-\\d{4}-[A-F0-9]{12}");

    private final Path rootPath;

    public LocalDocumentStorageAdapter(AppProperties properties) {
        this.rootPath = Path.of(properties.storage().rootPath())
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public DocumentMetadata store(
            String uploadId,
            String originalName,
            String mimeType,
            long sizeBytes,
            InputStream content) throws IOException {
        Path sessionDirectory = sessionDirectory(uploadId);
        log.debug("Creating local upload directory: uploadId={}", uploadId);
        Files.createDirectories(sessionDirectory);

        String safeName = safeFilename(originalName);
        Path target = uniqueTarget(sessionDirectory, safeName);
        log.debug(
                "Writing local document: uploadId={}, sizeBytes={}, mimeType={}",
                uploadId,
                sizeBytes,
                mimeType);
        Files.copy(content, target);
        String storageKey = rootPath.relativize(target).toString();
        log.debug("Local document write completed: uploadId={}, sizeBytes={}", uploadId, sizeBytes);

        return new DocumentMetadata(
                safeName,
                mimeType,
                storageKey,
                sizeBytes);
    }

    @Override
    public byte[] read(DocumentMetadata document) throws IOException {
        Path target = rootPath.resolve(document.storageKey()).normalize();
        if (!target.startsWith(rootPath) || !Files.isRegularFile(target)) {
            throw new IOException("Stored document path is invalid or missing");
        }
        return Files.readAllBytes(target);
    }

    @Override
    public void deleteUpload(String uploadId) throws IOException {
        Path directory = sessionDirectory(uploadId);
        if (!Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private Path sessionDirectory(String uploadId) throws IOException {
        if (!UPLOAD_ID_PATTERN.matcher(uploadId).matches()) {
            throw new IOException("Upload identifier is invalid");
        }
        Path directory = rootPath.resolve(uploadId).normalize();
        if (!directory.startsWith(rootPath)) {
            throw new IOException("Upload directory is outside storage root");
        }
        return directory;
    }

    private String safeFilename(String originalName) throws IOException {
        String basename = originalName.replace('\\', '/');
        basename = basename.substring(basename.lastIndexOf('/') + 1);
        String safe = basename.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.isBlank() || safe.equals(".") || safe.equals("..")) {
            throw new IOException("Document filename is invalid");
        }
        if (safe.length() > 150) {
            String extension = safe.contains(".")
                    ? safe.substring(safe.lastIndexOf('.'))
                    : "";
            int extensionLength = Math.min(20, extension.length());
            String safeExtension = extension.substring(extension.length() - extensionLength);
            int basenameLength = Math.max(1, 150 - extensionLength);
            safe = safe.substring(0, basenameLength) + safeExtension;
        }
        return safe;
    }

    private Path uniqueTarget(Path directory, String filename) {
        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(directory) || !Files.exists(target)) return target;

        String extension = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.'))
                : "";
        String basename = extension.isEmpty()
                ? filename
                : filename.substring(0, filename.length() - extension.length());
        Path candidate;
        do {
            candidate = directory.resolve(
                    basename + "-" + UUID.randomUUID().toString().substring(0, 8) + extension);
        } while (Files.exists(candidate));
        return candidate;
    }
}
