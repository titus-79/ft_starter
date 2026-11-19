package com.ftstarter.infrastructure.n8n;

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
    private final AppProperties.N8n n8nProperties;
    private final ApplicantExtractionMapper mapper;
    private final Retry retry;

    public N8nClientAdapter(
            WebClient n8nWebClient,
            AppProperties properties,
            ApplicantExtractionMapper mapper,
            RetryRegistry retryRegistry) {
        this.webClient = n8nWebClient;
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
                "Sending document analysis to n8n: uploadId={}, documentCount={}",
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
                .uri("document-analysis")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, this::transientError)
                .onStatus(HttpStatusCode::is4xxClientError, this::rejectedRequest)
                .bodyToMono(N8nAnalysisResponse.class)
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
            throw new N8nAnalysisException("n8n returned an empty response");
        }
        ApplicantExtractionResponse result = mapper.normalize(response);
        log.info(
                "n8n analysis completed: uploadId={}, detectedDocuments={}",
                payload.uploadId(),
                result.documentsDetected().size());
        return result;
    }

    private Mono<? extends Throwable> transientError(ClientResponse response) {
        return response.releaseBody().thenReturn(
                new N8nTransientException("n8n returned a server error", null));
    }

    private Mono<? extends Throwable> rejectedRequest(ClientResponse response) {
        return response.releaseBody().thenReturn(
                new N8nAnalysisException("n8n rejected the analysis request"));
    }

}
