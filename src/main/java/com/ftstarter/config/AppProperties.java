package com.ftstarter.config;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.util.unit.DataSize;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Storage storage,
        Upload upload,
        N8n n8n,
        Cors cors) {

    public record Storage(String rootPath) {}

    public record Upload(DataSize maxFileSize, int maxFiles, List<String> allowedExtensions) {
        public Upload {
            allowedExtensions = allowedExtensions.stream()
                    .map(extension -> extension.toLowerCase(Locale.ROOT)
                            .replaceFirst("^\\.", ""))
                    .toList();
        }
    }

    public record N8n(
            String baseUrl,
            Duration timeout,
            Duration connectTimeout,
            Duration responseTimeout) {}

    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = List.copyOf(allowedOrigins);
        }
    }
}
