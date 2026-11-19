package com.ftstarter.shared.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final List<String> details;

    public ApiException(
            String code,
            String message,
            HttpStatus status,
            List<String> details,
            Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public ApiException(String code, String message, HttpStatus status) {
        this(code, message, status, List.of(), null);
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public List<String> details() {
        return details;
    }
}
