package com.YouSumback.service.client;

import com.YouSumback.config.OpenAIConfig;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAIClient {
    private final OpenAIConfig openAIConfig;
    private final WebClient webClient = WebClient.builder()
            .baseUrl(openAIConfig.getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAIConfig.getApiKey())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    public Mono<String> chat(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", openAIConfig.getModel(),
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        return webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> response
                        .path("choices")
                        .get(0)
                        .path("message")
                        .path("content")
                        .asText()
                );
    }
}
