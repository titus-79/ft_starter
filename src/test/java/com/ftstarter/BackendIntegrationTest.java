package com.ftstarter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ftstarter.domain.model.AnalysisPayload;
import com.ftstarter.domain.model.Applicant;
import com.ftstarter.domain.model.ApplicantExtractionResponse;
import com.ftstarter.domain.port.AnalysisWorkflowPort;
import com.ftstarter.infrastructure.rest.CorrelationIdFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;
import org.mockito.ArgumentCaptor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
class BackendIntegrationTest {

    private static final Path STORAGE_ROOT = createStorageRoot();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AnalysisWorkflowPort workflow;

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.root-path", STORAGE_ROOT::toString);
    }

    @AfterAll
    static void deleteTestUploads() throws IOException {
        try (var paths = Files.walk(STORAGE_ROOT)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    void uploadsAnalyzesAndRetrievesAnApplicantWithoutAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "cni.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[] {37, 80, 68, 70});
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .header(CorrelationIdFilter.HEADER_NAME, "integration-test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.documentCount").value(1))
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, "integration-test"))
                .andReturn();

        String uploadId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .get("uploadId")
                .asText();
        assertNotNull(uploadId);
        assertEquals(1, countFiles(STORAGE_ROOT.resolve(uploadId)));

        ApplicantExtractionResponse expected = new ApplicantExtractionResponse(
                0.94,
                List.of("CNI"),
                Applicant.empty());
        when(workflow.analyze(any(AnalysisPayload.class))).thenReturn(expected);

        mockMvc.perform(post("/api/v1/analysis/{uploadId}", uploadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confidenceScore").value(0.94))
                .andExpect(jsonPath("$.applicant.employmentCertificates").isArray());

        ArgumentCaptor<AnalysisPayload> payload = ArgumentCaptor.forClass(AnalysisPayload.class);
        verify(workflow).analyze(payload.capture());
        assertEquals("JVBERg==", payload.getValue().documents().getFirst().base64());

        mockMvc.perform(get("/api/v1/analysis/{uploadId}", uploadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentsDetected[0]").value("CNI"));
    }

    @Test
    void mapsMissingAnalysisToTheDocumentedNotFoundShape() throws Exception {
        mockMvc.perform(get("/api/v1/analysis/UPL-2026-unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No analysis found for uploadId"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.details[0]").value("UPL-2026-unknown"));
    }

    @Test
    void rejectsUnsupportedFilesWithTheStandardErrorBody() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files[]",
                "script.exe",
                "application/octet-stream",
                new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/v1/documents/upload").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_EXTENSION"))
                .andExpect(jsonPath("$.details[0]").value("script.exe"));
    }

    @Test
    void exposesHealthWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void exposesOpenApiDocumentationWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("FT Starter API"));
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("FT Starter Backend"));
    }

    private static Path createStorageRoot() {
        try {
            return Files.createTempDirectory("ft-starter-integration-");
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static long countFiles(Path directory) throws IOException {
        try (var paths = Files.list(directory)) {
            return paths.count();
        }
    }
}
