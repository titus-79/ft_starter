package com.ftstarter.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidDocumentException extends ApiException {

    public InvalidDocumentException(String message) {
        super("INVALID_DOCUMENT", message, HttpStatus.BAD_REQUEST);
    }
}
