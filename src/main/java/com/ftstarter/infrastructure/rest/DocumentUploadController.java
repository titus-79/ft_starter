package com.ftstarter.infrastructure.rest;

import com.ftstarter.application.usecase.IncomingDocument;
import com.ftstarter.application.usecase.UploadDocumentsUseCase;
import com.ftstarter.domain.model.UploadSession;
import com.ftstarter.infrastructure.rest.dto.UploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents")
public class DocumentUploadController {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadController.class);

    private final UploadDocumentsUseCase uploadDocuments;

    public DocumentUploadController(UploadDocumentsUseCase uploadDocuments) {
        this.uploadDocuments = uploadDocuments;
    }

    @PostMapping(
            path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Déposer des documents",
            description = "Accepte jusqu'à 20 fichiers PDF, PNG, JPG ou JPEG de 20 Mo maximum chacun.")
    @ApiResponse(
            responseCode = "201",
            description = "Documents stockés localement",
            content = @Content(schema = @Schema(implementation = UploadResponse.class)))
    @ApiResponse(responseCode = "400", description = "Requête ou document invalide")
    @ApiResponse(responseCode = "413", description = "Taille maximale dépassée")
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("files[]") List<MultipartFile> files) {
        int documentCount = files.size();
        long totalSizeBytes = files.stream().mapToLong(MultipartFile::getSize).sum();
        log.info(
                "Document upload request received: documentCount={}, totalSizeBytes={}",
                documentCount,
                totalSizeBytes);
        List<IncomingDocument> incomingDocuments = files.stream()
                .map(file -> new IncomingDocument(
                        file.getOriginalFilename(),
                        file.getSize(),
                        file::getInputStream))
                .toList();
        UploadSession session;
        try {
            session = uploadDocuments.execute(incomingDocuments);
        } catch (RuntimeException exception) {
            log.warn(
                    "Document upload request failed: documentCount={}, exceptionType={}",
                    documentCount,
                    exception.getClass().getSimpleName());
            throw exception;
        }
        log.info(
                "Document upload request completed: uploadId={}, documentCount={}",
                session.uploadId(),
                session.documents().size());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UploadResponse.from(session));
    }
}
