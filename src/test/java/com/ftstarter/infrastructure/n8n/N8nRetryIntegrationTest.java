package com.ftstarter.infrastructure.n8n;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.ftstarter.domain.model.AnalysisDocument;
import com.ftstarter.domain.model.AnalysisPayload;
import com.ftstarter.domain.port.AnalysisWorkflowPort;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = MOCK)
class N8nRetryIntegrationTest {

    private static final WireMockServer wireMock = startWireMock();

    @Autowired
    private AnalysisWorkflowPort workflow;

    @DynamicPropertySource
    static void n8nProperties(DynamicPropertyRegistry registry) {
        registry.add("app.n8n.base-url", () -> wireMock.baseUrl() + "/webhook");
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
    void retriesTransientN8nErrorsAndReturnsTheNormalizedResult() {
        wireMock.stubFor(post(urlEqualTo("/webhook/document-analysis"))
                .inScenario("retry-once")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("available"));
        wireMock.stubFor(post(urlEqualTo("/webhook/document-analysis"))
                .inScenario("retry-once")
                .whenScenarioStateIs("available")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "confidenceScore": 0.94,
                                  "documentsDetected": ["CNI"],
                                  "applicant": {"firstName": "Jean"}
                                }
                                """)));

        var result = workflow.analyze(new AnalysisPayload(
                "UPL-2026-000000000001",
                List.of(new AnalysisDocument("cni.pdf", "application/pdf", "JVBERg=="))));

        assertEquals(0.94, result.confidenceScore());
        assertEquals("Jean", result.applicant().firstName());
        wireMock.verify(2, postRequestedFor(urlEqualTo("/webhook/document-analysis")));
    }

    private static WireMockServer startWireMock() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        return server;
    }
}
