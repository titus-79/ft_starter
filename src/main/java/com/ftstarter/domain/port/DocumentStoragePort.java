package com.ftstarter.domain.port;

import com.ftstarter.domain.model.DocumentMetadata;
import java.io.IOException;
import java.io.InputStream;

public interface DocumentStoragePort {

    DocumentMetadata store(
            String uploadId,
            String originalName,
            String mimeType,
            long sizeBytes,
            InputStream content) throws IOException;

    byte[] read(DocumentMetadata document) throws IOException;

    void deleteUpload(String uploadId) throws IOException;
}
