package com.ftstarter.shared.exception;

import org.springframework.http.HttpStatus;

public class DocumentTooLargeException extends ApiException {

    public DocumentTooLargeException(String filename, long maxSize) {
        super(
                "DOCUMENT_TOO_LARGE",
                "Maximum size exceeded",
                HttpStatus.PAYLOAD_TOO_LARGE,
                java.util.List.of(filename, "Maximum size: " + maxSize + " bytes"),
                null);
    }
}
