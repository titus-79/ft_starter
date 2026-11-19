package com.ftstarter.shared.exception;

import org.springframework.http.HttpStatus;

public class N8nTimeoutException extends ApiException {

    public N8nTimeoutException(String message, Throwable cause) {
        super("N8N_TIMEOUT", message, HttpStatus.GATEWAY_TIMEOUT, null, cause);
    }
}
