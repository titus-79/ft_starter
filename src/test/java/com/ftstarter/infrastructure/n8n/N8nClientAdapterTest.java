package com.ftstarter.infrastructure.n8n;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ftstarter.config.AppProperties;
import com.ftstarter.domain.model.AnalysisDocument;
import com.ftstarter.domain.model.AnalysisPayload;
import com.ftstarter.infrastructure.mapper.ApplicantExtractionMapper;
import com.ftstarter.shared.exception.N8nAnalysisException;
import com.ftstarter.shared.exception.N8nTimeoutException;
import com.ftstarter.shared.exception.N8nTransientException;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.List;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import org.springframework.util.unit.DataSize;

class N8nClientAdapterTest {

    private static final WireMockServer wireMock =
            new WireMockServer(options().dynamicPort());

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Test
    void postsBase64DocumentsAndNormalizesN8nResponse() {
        wireMock.stubFor(post(urlEqualTo("/webhook/document-analysis"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "confidenceScore": 0.91,
                                  "documentsDetected": ["cni", "Attestation employeur"],
                                  "applicant": {
                                    "firstName": " Jean ",
                                    "employmentCertificates": [{
                                      "employerName": "Capgemini",
                                      "siret": "38012986600014",
                                      "grossSalary": 38000
                                    }]
                                  }
                                }
                                """)));

        var result = adapter(Duration.ofSeconds(2)).analyze(payload());

        assertEquals(0.91, result.confidenceScore());
        assertEquals(List.of("CNI", "ATTESTATION_EMPLOYEUR"), result.documentsDetected());
        assertEquals("Jean", result.applicant().firstName());
        assertEquals(
                "Capgemini",
                result.applicant().employmentCertificates().getFirst().employerName());
        wireMock.verify(postRequestedFor(urlEqualTo("/webhook/document-analysis"))
                .withRequestBody(
                        com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath(
                                "$.documents[0].base64",
                                com.github.tomakehurst.wiremock.client.WireMock.equalTo("cGRm"))));
    }

    @Test
    void convertsWorkflowServerErrorsToRetriableErrors() {
        wireMock.stubFor(post(urlEqualTo("/webhook/document-analysis"))
                .willReturn(aResponse().withStatus(503)));

        assertThrows(
                N8nTransientException.class,
                () -> adapter(Duration.ofSeconds(2)).analyze(payload()));
    }

    @Test
    void convertsClientErrorsToGatewayErrorsWithoutRetryClassification() {
        wireMock.stubFor(post(urlEqualTo("/webhook/document-analysis"))
                .willReturn(aResponse().withStatus(400)));

        assertThrows(
                N8nAnalysisException.class,
                () -> adapter(Duration.ofSeconds(2)).analyze(payload()));
    }

    @Test
    void appliesTheConfiguredWorkflowTimeout() {
        wireMock.stubFor(post(urlEqualTo("/webhook/document-analysis"))
                .willReturn(aResponse().withFixedDelay(500).withStatus(200)));

        assertThrows(
                N8nTimeoutException.class,
                () -> adapter(Duration.ofMillis(100)).analyze(payload()));
    }

    private N8nClientAdapter adapter(Duration timeout) {
        AppProperties properties = new AppProperties(
                new AppProperties.Storage("uploads"),
                new AppProperties.Upload(DataSize.ofBytes(1024), 20, List.of("pdf")),
                new AppProperties.N8n(
                        wireMock.baseUrl() + "/webhook",
                        timeout,
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(2)),
                new AppProperties.Cors(List.of()));
        WebClient webClient = WebClient.builder()
                .baseUrl(wireMock.baseUrl() + "/webhook/")
                .build();
        return new N8nClientAdapter(
                webClient,
                properties,
                new ApplicantExtractionMapper(),
                RetryRegistry.of(RetryConfig.custom().maxAttempts(1).build()));
    }

    private AnalysisPayload payload() {
        return new AnalysisPayload(
                "UPL-2026-000000000001",
                List.of(new AnalysisDocument("cni.pdf", "application/pdf", "cGRm")));
    }
}
