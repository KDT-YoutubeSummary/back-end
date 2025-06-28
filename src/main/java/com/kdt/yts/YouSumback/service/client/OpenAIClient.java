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
import java.util.Objects;

@Component
public class OpenAIClient {
    private final OpenAIConfig openAIConfig;
    private final WebClient webClient;
    private final String model; // ✅ 멤버 변수로 모델을 저장

    public OpenAIClient(OpenAIConfig openAIConfig) {
        this.openAIConfig = openAIConfig;
        this.webClient = WebClient.builder()
                .baseUrl(openAIConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        // ✅ 생성자에서 모델을 한 번만 가져오고, null일 경우 기본값을 설정합니다.
        this.model = Objects.requireNonNullElse(openAIConfig.getModel(), "gpt-4-turbo");

        System.out.println("✅ [OpenAI 설정 확인]");
        System.out.println("Base URL: " + openAIConfig.getBaseUrl());
        System.out.println("API Key: " + (openAIConfig.getApiKey() != null ? "설정됨" : "설정되지 않음"));
        System.out.println("Model: " + this.model);
    }


    public Mono<String> chat(String prompt) {
        // ✅ Null 체크 로직을 더 명확하게 수정합니다.
        if (prompt == null) {
            return Mono.error(new IllegalArgumentException("Prompt가 null입니다."));
        }

        // ✅ 메시지 객체 생성
        Map<String, Object> message = Map.of(
                "role", "user",
                "content", prompt
        );

        // ✅ 요청 바디 객체 생성 (멤버 변수 model 사용)
        Map<String, Object> requestBody = Map.of(
                "model", this.model,
                "messages", List.of(message)
        );

        return webClient.post()
                .uri("/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIConfig.getApiKey())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.get("choices").get(0).get("message").get("content").asText());
    }
}