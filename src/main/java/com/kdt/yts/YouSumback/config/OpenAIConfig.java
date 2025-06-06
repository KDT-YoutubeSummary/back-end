package com.kdt.yts.YouSumback.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "openai")
@Data
public class OpenAIConfig {
    private String apiKey;
    private String baseUrl;
    private String model;

    @PostConstruct
    public void init() {
        System.out.println("✅ [OpenAIConfig] apiKey: " + apiKey);
        System.out.println("✅ [OpenAIConfig] baseUrl: " + baseUrl);
        System.out.println("✅ [OpenAIConfig] model: " + model);
    }
}

