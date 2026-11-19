package com.ftstarter.domain.model;

public record DocumentMetadata(
        String name,
        String mimeType,
        String storageKey,
        long sizeBytes) {}
