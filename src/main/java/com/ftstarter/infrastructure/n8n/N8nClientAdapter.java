package com.ftstarter.infrastructure.n8n;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ftstarter.config.AppProperties;
import com.ftstarter.domain.model.AnalysisPayload;
import com.ftstarter.domain.model.ApplicantExtractionResponse;
import com.ftstarter.domain.port.AnalysisWorkflowPort;
import com.ftstarter.infrastructure.mapper.ApplicantExtractionMapper;
import com.ftstarter.shared.exception.N8nAnalysisException;
import com.ftstarter.shared.exception.N8nTimeoutException;
import com.ftstarter.shared.exception.N8nTransientException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@Component
public class N8nClientAdapter implements AnalysisWorkflowPort {

    private static final Logger log = LoggerFactory.getLogger(N8nClientAdapter.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AppProperties.N8n n8nProperties;
    private final ApplicantExtractionMapper mapper;
    private final Retry retry;

    public N8nClientAdapter(
            WebClient n8nWebClient,
            ObjectMapper objectMapper,
            AppProperties properties,
            ApplicantExtractionMapper mapper,
            RetryRegistry retryRegistry) {
        this.webClient = n8nWebClient;
        this.objectMapper = objectMapper;
        this.n8nProperties = properties.n8n();
        this.mapper = mapper;
        this.retry = retryRegistry.retry("n8nAnalysis");
    }

    @Override
    public ApplicantExtractionResponse analyze(AnalysisPayload payload) {
        try {
            return retry.executeSupplier(() -> sendAnalysis(payload));
        } catch (N8nAnalysisException | N8nTimeoutException exception) {
            log.warn(
                    "n8n analysis failed: uploadId={}, code={}, exceptionType={}",
                    payload.uploadId(),
                    exception instanceof N8nTimeoutException ? "N8N_TIMEOUT" : "N8N_ERROR",
                    exception.getClass().getSimpleName());
            throw exception;
        } catch (DecodingException exception) {
            log.error(
                    "n8n response decoding failed: uploadId={}, exceptionType={}",
                    payload.uploadId(),
                    exception.getClass().getSimpleName());
            throw new N8nAnalysisException(
                    "n8n response does not match the expected JSON format",
                    exception);
        } catch (RuntimeException exception) {
            log.error(
                    "Unexpected n8n client error: uploadId={}, exceptionType={}",
                    payload.uploadId(),
                    exception.getClass().getSimpleName());
            throw new N8nAnalysisException("Could not process n8n response", exception);
        }
    }

    private ApplicantExtractionResponse sendAnalysis(AnalysisPayload payload) {
        log.info(
                "n8n webhook request started: uploadId={}, documentCount={}",
                payload.uploadId(),
                payload.documents().size());
        N8nAnalysisRequest request = new N8nAnalysisRequest(
                payload.uploadId(),
                payload.documents().stream()
                        .map(document -> new N8nDocumentPayload(
                                document.name(),
                                document.mimeType(),
                                document.base64()))
                        .toList());

        N8nAnalysisResponse response = webClient.post()
                .uri("analyze")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        clientResponse -> transientError(clientResponse, payload.uploadId()))
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        clientResponse -> rejectedRequest(clientResponse, payload.uploadId()))
                .bodyToMono(JsonNode.class)
                .map(responseBody -> decodeResponse(responseBody, payload.uploadId()))
                .doOnNext(n8nResponse -> log.info(
                        "n8n webhook response decoded: uploadId={}, detectedDocumentCount={}",
                        payload.uploadId(),
                        n8nResponse.documentsDetected() == null
                                ? 0
                                : n8nResponse.documentsDetected().size()))
                .timeout(n8nProperties.timeout())
                .onErrorMap(
                        TimeoutException.class,
                        exception -> new N8nTimeoutException(
                                "n8n analysis timed out",
                                exception))
                .onErrorMap(
                        WebClientRequestException.class,
                        exception -> new N8nTransientException(
                                "Could not connect to n8n",
                                exception))
                .block(n8nProperties.timeout().plusSeconds(1));

        if (response == null) {
            log.error("n8n webhook returned an empty response: uploadId={}", payload.uploadId());
            throw new N8nAnalysisException("n8n returned an empty response");
        }
        ApplicantExtractionResponse result = mapper.normalize(response);
        log.info(
                "n8n analysis completed: uploadId={}, detectedDocuments={}",
                payload.uploadId(),
                result.documentsDetected().size());
        return result;
    }

    private N8nAnalysisResponse decodeResponse(JsonNode responseBody, String uploadId) {
        JsonNode response = responseBody;
        if (response == null || response.isNull()) {
            throw new N8nAnalysisException("n8n returned an empty response");
        }

        if (response.isArray()) {
            if (response.size() != 1) {
                throw new N8nAnalysisException(
                        "n8n response array must contain exactly one result");
            }
            response = response.get(0);
        }

        JsonNode output = response.path("output");
        if (!output.isMissingNode() && !output.isNull()) {
            String outputText = findOutputText(output);
            if (outputText != null) {
                log.info("Unwrapping structured JSON from n8n output: uploadId={}", uploadId);
                return parseResponseText(outputText);
            }
            response = output;
        }

        try {
            if (response.isTextual()) {
                return parseResponseText(response.asText());
            }
            return objectMapper.treeToValue(response, N8nAnalysisResponse.class);
        } catch (JsonProcessingException exception) {
            throw new N8nAnalysisException(
                    "n8n response does not match the expected JSON format",
                    exception);
        }
    }

    private N8nAnalysisResponse parseResponseText(String responseText) {
        try {
            return objectMapper.readValue(responseText, N8nAnalysisResponse.class);
        } catch (JsonProcessingException exception) {
            throw new N8nAnalysisException(
                    "n8n output text is not valid applicant JSON",
                    exception);
        }
    }

    private String findOutputText(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isArray()) {
            for (JsonNode item : node) {
                String text = findOutputText(item);
                if (text != null) return text;
            }
            return null;
        }
        if (!node.isObject()) return null;

        JsonNode type = node.get("type");
        JsonNode text = node.get("text");
        if (text != null
                && text.isTextual()
                && (type == null || "output_text".equals(type.asText()))) {
            return text.asText();
        }

        String contentText = findOutputText(node.get("content"));
        return contentText != null ? contentText : findOutputText(node.get("output"));
    }

    private Mono<? extends Throwable> transientError(ClientResponse response, String uploadId) {
        log.warn(
                "n8n webhook returned a server error: uploadId={}, status={}",
                uploadId,
                response.statusCode().value());
        return response.releaseBody().thenReturn(
                new N8nTransientException("n8n returned a server error", null));
    }

    private Mono<? extends Throwable> rejectedRequest(ClientResponse response, String uploadId) {
        log.warn(
                "n8n webhook rejected the request: uploadId={}, status={}",
                uploadId,
                response.statusCode().value());
        return response.releaseBody().thenReturn(
                new N8nAnalysisException("n8n rejected the analysis request"));
    }

}
