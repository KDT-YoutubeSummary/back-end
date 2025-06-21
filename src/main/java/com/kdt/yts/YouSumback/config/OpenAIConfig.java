package com.kdt.yts.YouSumback.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "openai")
@Getter
@Setter
public class OpenAIConfig {
    private String apiKey;
    private String model;
    private String baseUrl;
}

