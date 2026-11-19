package com.ftstarter.infrastructure.rest;

import com.ftstarter.infrastructure.rest.dto.ErrorResponse;
import com.ftstarter.shared.exception.ApiException;
import java.time.Instant;
import java.util.List;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        log.warn(
                "API request rejected: code={}, status={}, exceptionType={}",
                exception.code(),
                exception.status().value(),
                exception.getClass().getSimpleName());
        return ResponseEntity.status(exception.status())
                .body(new ErrorResponse(
                        Instant.now(),
                        exception.code(),
                        exception.getMessage(),
                        exception.details()));
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        ConstraintViolationException.class,
        MissingServletRequestParameterException.class,
        MissingServletRequestPartException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleValidationException(Exception exception) {
        log.warn(
                "API request validation failed: exceptionType={}",
                exception.getClass().getSimpleName());
        List<String> details;
        if (exception instanceof MethodArgumentNotValidException validationException) {
            details = validationException.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .toList();
        } else if (exception instanceof BindException bindException) {
            details = bindException.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .toList();
        } else if (exception instanceof ConstraintViolationException violationException) {
            details = violationException.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + ": "
                            + violation.getMessage())
                    .toList();
        } else if (exception instanceof MissingServletRequestParameterException missing) {
            details = List.of("Missing parameter: " + missing.getParameterName());
        } else if (exception instanceof MissingServletRequestPartException missing) {
            details = List.of("Missing part: " + missing.getRequestPartName());
        } else if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            details = List.of("Invalid parameter: " + mismatch.getName());
        } else if (exception instanceof HttpMessageNotReadableException) {
            details = List.of("Request body is missing or malformed");
        } else {
            details = List.of();
        }
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                details);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException exception) {
        log.warn(
                "Document upload rejected: code=DOCUMENT_TOO_LARGE, exceptionType={}",
                exception.getClass().getSimpleName());
        return error(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "DOCUMENT_TOO_LARGE",
                "Maximum request size exceeded",
                List.of());
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(
            MultipartException exception) {
        log.warn(
                "Document upload rejected: code=INVALID_DOCUMENT, exceptionType={}",
                exception.getClass().getSimpleName());
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_DOCUMENT",
                "Multipart request is invalid",
                List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        log.error(
                "Unhandled API error: exceptionType={}",
                exception.getClass().getSimpleName());
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                List.of());
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            List<String> details) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(Instant.now(), code, message, details));
    }
}
