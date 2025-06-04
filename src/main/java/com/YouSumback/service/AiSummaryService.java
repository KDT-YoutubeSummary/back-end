package com.YouSumback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiSummaryService {

    private final ChatClient chatClient;

    public String summarize(String inputText) {
        return chatClient.call(inputText); // Spring AI 방식 요약
    }
}
