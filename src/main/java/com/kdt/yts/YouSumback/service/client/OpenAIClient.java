package com.kdt.yts.YouSumback.service.client;

import com.kdt.yts.YouSumback.config.OpenAIConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class OpenAIClient {
    private final OpenAIConfig openAIConfig;
    private final WebClient webClient;

    public OpenAIClient(OpenAIConfig openAIConfig) {
        this.openAIConfig = openAIConfig;
        this.webClient = WebClient.builder()
                .baseUrl(openAIConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAIConfig.getApiKey())
                .build();
    }

    public Mono<String> chat(String prompt) {
        return webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(Map.of(
                        "model", openAIConfig.getModel(),
                        "messages", List.of(Map.of(
                                "role", "user",
                                "content", prompt
                        ))
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.get("choices").get(0).get("message").get("content").asText());
    }
}


