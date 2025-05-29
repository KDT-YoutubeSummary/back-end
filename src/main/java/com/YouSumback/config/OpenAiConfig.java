package com.YouSumback.config;

import com.theokanning.openai.service.OpenAiService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @PostConstruct
    public void printKey() {
        System.out.println("✅ loaded openAiApiKey: " + openAiApiKey);
    }

    @Bean
    public OpenAiService openAiService() {
        return new OpenAiService(openAiApiKey); // 여기서 null이면 NPE 발생
    }
}
