package com.ftstarter.shared.exception;

import org.springframework.http.HttpStatus;

public class UnsupportedFileExtensionException extends ApiException {

    public UnsupportedFileExtensionException(String filename) {
        super(
                "UNSUPPORTED_FILE_EXTENSION",
                "File extension is not allowed",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                java.util.List.of(filename),
                null);
    }
}
