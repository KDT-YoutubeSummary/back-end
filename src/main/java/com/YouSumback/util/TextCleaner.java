package com.YouSumback.util;

import org.springframework.stereotype.Component;

@Component
public class TextCleaner {

    public String clean(String rawText) {
        if (rawText == null || rawText.isBlank()) return "";

        // 1. [음악], [웃음] 같은 텍스트 제거
        rawText = rawText.replaceAll("\\[.*?\\]", "");

        // 2. 감탄사 및 불필요한 소리 제거
        String[] fillers = {"어\\?", "오!", "아이고", "아이구", "응!", "히우!", "크!", "흠", "휴!", "아!", "우와!", "와!", "헉!"};
        for (String filler : fillers) {
            rawText = rawText.replaceAll(filler, "");
        }

        // 3. 중복 단어 제거 예: "반갑! 반갑!" → "반갑!"
        rawText = rawText.replaceAll("\\b(\\w+)([!?])\\s+\\1\\2", "$1$2");

        // 4. 조사 앞 공백 제거 (예: "종이 는" → "종이는")
        rawText = rawText.replaceAll(" ([은는이가에도를])", "$1");

        // 5. 문장 끝 처리 (간단한 문장 구분용)
        rawText = rawText.replaceAll("([가-힣])\\s+", "$1. ");

        // 6. 마침표 중복 제거 및 전체 공백 정리
        rawText = rawText.replaceAll("\\.\\s*\\.", ".").replaceAll("\\s+", " ").trim();

        return rawText;
    }
}
