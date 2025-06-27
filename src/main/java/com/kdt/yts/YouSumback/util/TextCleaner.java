package com.kdt.yts.YouSumback.util;

import org.springframework.stereotype.Component;

@Component
public class TextCleaner {

    public String clean(String input) {
        if (input == null || input.isBlank()) return "";
        return input.replaceAll("\\s+", " ") // 연속 공백 제거
                .replaceAll("\\[[^]]*]", "") // 대괄호 제거
                .trim();
    }
}