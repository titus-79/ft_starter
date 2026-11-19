package com.ftstarter.shared.exception;

import org.springframework.http.HttpStatus;

public class StorageException extends ApiException {

    public StorageException(String message, Throwable cause) {
        super("STORAGE_ERROR", message, HttpStatus.INTERNAL_SERVER_ERROR, null, cause);
    }
}
