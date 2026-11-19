package com.ftstarter.infrastructure.rest.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        String code,
        String message,
        List<String> details) {

    public ErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
