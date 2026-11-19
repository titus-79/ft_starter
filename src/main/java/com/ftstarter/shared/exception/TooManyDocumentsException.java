package com.ftstarter.shared.exception;

import org.springframework.http.HttpStatus;

public class TooManyDocumentsException extends ApiException {

    public TooManyDocumentsException(int maxFiles) {
        super(
                "TOO_MANY_DOCUMENTS",
                "The maximum number of files has been exceeded",
                HttpStatus.BAD_REQUEST,
                java.util.List.of("Maximum number of files: " + maxFiles),
                null);
    }
}
