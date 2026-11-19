package com.ftstarter.shared.exception;

import org.springframework.http.HttpStatus;

public class UploadNotFoundException extends ApiException {

    public UploadNotFoundException(String uploadId) {
        super(
                "UPLOAD_NOT_FOUND",
                "No upload found for uploadId",
                HttpStatus.NOT_FOUND,
                java.util.List.of(uploadId),
                null);
    }
}
