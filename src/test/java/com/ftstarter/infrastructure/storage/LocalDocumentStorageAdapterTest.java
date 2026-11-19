package com.ftstarter.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ftstarter.config.AppProperties;
import com.ftstarter.domain.model.DocumentMetadata;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

class LocalDocumentStorageAdapterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesSafeUniqueNamesAndReadsTheExactBytes() throws Exception {
        LocalDocumentStorageAdapter storage = new LocalDocumentStorageAdapter(properties());
        byte[] expected = new byte[] {1, 2, 3};

        DocumentMetadata first = storage.store(
                "UPL-2026-000000000001",
                "..\\private\\cni.pdf",
                "application/pdf",
                expected.length,
                new ByteArrayInputStream(expected));
        DocumentMetadata second = storage.store(
                "UPL-2026-000000000001",
                "..\\private\\cni.pdf",
                "application/pdf",
                expected.length,
                new ByteArrayInputStream(expected));

        assertTrue(Path.of(first.storageKey()).getFileName().toString().startsWith("cni"));
        assertNotEquals(first.storageKey(), second.storageKey());
        assertArrayEquals(expected, storage.read(first));
        assertTrue(Files.isRegularFile(
                temporaryDirectory.resolve("UPL-2026-000000000001").resolve(
                        Path.of(first.storageKey()).getFileName())));

        storage.deleteUpload("UPL-2026-000000000001");
        assertFalse(Files.exists(temporaryDirectory.resolve("UPL-2026-000000000001")));
    }

    private AppProperties properties() {
        return new AppProperties(
                new AppProperties.Storage(temporaryDirectory.toString()),
                new AppProperties.Upload(DataSize.ofBytes(1024), 20, List.of("pdf")),
                null,
                new AppProperties.Cors(List.of()));
    }
}
