package com.ftstarter.application.usecase;

import java.io.IOException;
import java.io.InputStream;

public record IncomingDocument(
        String originalName,
        long sizeBytes,
        InputStreamSupplier content) {

    @FunctionalInterface
    public interface InputStreamSupplier {
        InputStream open() throws IOException;
    }
}
