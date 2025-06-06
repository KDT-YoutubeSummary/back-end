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
        // ✅ 방어코딩: null 방지
        String model = openAIConfig.getModel();
        if (model == null || prompt == null) {
            return Mono.error(new IllegalArgumentException("Model 또는 Prompt가 null입니다."));
        }

        // ✅ 메시지 객체 생성
        Map<String, Object> message = Map.of(
                "role", "user",
                "content", prompt
        );

        // ✅ 요청 바디 객체 생성
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(message)
        );

        return webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.get("choices").get(0).get("message").get("content").asText());
    }

}


