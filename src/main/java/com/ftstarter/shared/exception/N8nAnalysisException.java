package com.ftstarter.shared.exception;

import org.springframework.http.HttpStatus;

public class N8nAnalysisException extends ApiException {

    public N8nAnalysisException(String message) {
        super("N8N_ERROR", message, HttpStatus.BAD_GATEWAY);
    }

    public N8nAnalysisException(String message, Throwable cause) {
        super("N8N_ERROR", message, HttpStatus.BAD_GATEWAY, null, cause);
    }
}
