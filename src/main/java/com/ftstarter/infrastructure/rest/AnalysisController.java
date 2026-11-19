package com.ftstarter.infrastructure.rest;

import com.ftstarter.application.usecase.AnalyzeDocumentsUseCase;
import com.ftstarter.application.usecase.GetAnalysisResultUseCase;
import com.ftstarter.domain.model.ApplicantExtractionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis")
@Tag(name = "Analysis")
public class AnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);

    private final AnalyzeDocumentsUseCase analyzeDocuments;
    private final GetAnalysisResultUseCase getAnalysisResult;

    public AnalysisController(
            AnalyzeDocumentsUseCase analyzeDocuments,
            GetAnalysisResultUseCase getAnalysisResult) {
        this.analyzeDocuments = analyzeDocuments;
        this.getAnalysisResult = getAnalysisResult;
    }

    @PostMapping(
            path = "/{uploadId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Analyser les documents",
            description = "Transmet les documents encodés en Base64 au workflow n8n.")
    @ApiResponse(responseCode = "200", description = "Analyse documentaire normalisée")
    @ApiResponse(responseCode = "404", description = "Session de dépôt introuvable")
    @ApiResponse(responseCode = "502", description = "Erreur du workflow n8n")
    @ApiResponse(responseCode = "504", description = "Délai du workflow n8n dépassé")
    public ApplicantExtractionResponse analyze(@PathVariable String uploadId) {
        log.info("Analysis API request received: uploadId={}", uploadId);
        try {
            ApplicantExtractionResponse result = analyzeDocuments.execute(uploadId);
            log.info(
                    "Analysis API request completed: uploadId={}, detectedDocumentCount={}",
                    uploadId,
                    result.documentsDetected().size());
            return result;
        } catch (RuntimeException exception) {
            log.warn(
                    "Analysis API request failed: uploadId={}, exceptionType={}",
                    uploadId,
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    @GetMapping(path = "/{uploadId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulter le résultat d’analyse")
    @ApiResponse(responseCode = "200", description = "Résultat trouvé")
    @ApiResponse(responseCode = "404", description = "Résultat d’analyse introuvable")
    public ApplicantExtractionResponse getResult(@PathVariable String uploadId) {
        return getAnalysisResult.execute(uploadId);
    }
}
