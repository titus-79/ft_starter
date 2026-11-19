package com.ftstarter.application.service;

import java.time.Clock;
import java.time.Year;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UploadIdGenerator {

    private final Clock clock;

    public UploadIdGenerator(Clock clock) {
        this.clock = clock;
    }

    public String generate() {
        String suffix = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase();
        return "UPL-" + Year.now(clock) + "-" + suffix;
    }
}
