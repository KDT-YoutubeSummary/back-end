package com.kdt.yts.YouSumback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiSummaryService {

    private final ChatClient chatClient;

    public String summarize(String inputText) {
        return chatClient.prompt()
                .user("다음 내용을 바탕으로 요약해줘:\n" + inputText)
                .call()
                .content();

    }
}
