package com.ftstarter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI ftStarterOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FT Starter API")
                .version("v1")
                .description("""
                        API de démonstration de réception des justificatifs et d'orchestration \
                        d'une analyse documentaire déléguée à n8n. Aucun OCR n'est exécuté par \
                        le backend.
                        """));
    }
}
