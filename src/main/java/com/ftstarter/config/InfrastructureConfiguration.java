package com.ftstarter.config;

import io.netty.channel.ChannelOption;
import com.ftstarter.infrastructure.rest.CorrelationIdFilter;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class InfrastructureConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    WebClient n8nWebClient(WebClient.Builder builder, AppProperties properties) {
        AppProperties.N8n n8n = properties.n8n();
        HttpClient httpClient = HttpClient.create()
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        Math.toIntExact(n8n.connectTimeout().toMillis()))
                .responseTimeout(n8n.responseTimeout());
        String baseUrl = n8n.baseUrl().replaceAll("/+$", "") + "/";

        return builder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(correlationIdFilter())
                .build();
    }

    private ExchangeFilterFunction correlationIdFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (correlationId == null || correlationId.isBlank()) return reactor.core.publisher.Mono.just(request);
            ClientRequest requestWithCorrelationId = ClientRequest.from(request)
                    .headers(headers -> headers.set(CorrelationIdFilter.HEADER_NAME, correlationId))
                    .build();
            return reactor.core.publisher.Mono.just(requestWithCorrelationId);
        });
    }
}
