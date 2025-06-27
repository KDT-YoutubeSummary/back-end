package com.kdt.yts.YouSumback.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextCleanerTest {

    private final TextCleaner textCleaner = new TextCleaner();

    @Test
    @DisplayName("기본적인 텍스트 정제 테스트")
    void testBasicCleaning() {
        String input = "  [Music]  Hello world! This is a test...  Thank you.  ";
        String expected = "Hello world! This is a test... Thank you.";
        assertEquals(expected, textCleaner.clean(input));
    }

    @Test
    @DisplayName("괄호 안의 단어는 제거되지 않음")
    void testRemoveBracketedWords() {
        String input = "(Laughter) What a funny joke! [Applause]";
        String expected = "(Laughter) What a funny joke!";
        assertEquals(expected, textCleaner.clean(input));
    }

    @Test
    @DisplayName("반복되는 공백 및 줄바꿈 제거 테스트")
    void testRemoveExtraSpacesAndNewlines() {
        String input = "First line.\n\nSecond line.   Third line.";
        String expected = "First line. Second line. Third line.";
        assertEquals(expected, textCleaner.clean(input));
    }

    @Test
    @DisplayName("특수 문자 및 이모지는 제거되지 않음")
    void testRemoveSpecialCharacters() {
        String input = "This is great! 😊👍 Is it?";
        String expected = "This is great! 😊👍 Is it?";
        assertEquals(expected, textCleaner.clean(input));
    }

    @Test
    @DisplayName("URL은 제거되지 않음")
    void testRemoveUrls() {
        String input = "Check out this link: https://example.com or www.test.com.";
        String expected = "Check out this link: https://example.com or www.test.com.";
        assertEquals(expected, textCleaner.clean(input));
    }

    @Test
    @DisplayName("비어 있거나 공백만 있는 문자열 테스트")
    void testEmptyAndWhitespaceString() {
        String input = "   \n \t  ";
        String expected = "";
        assertEquals(expected, textCleaner.clean(input));
    }

    @Test
    @DisplayName("여러 정제 규칙이 복합된 문자열 테스트")
    void testComplexString() {
        String input = "[Music] Welcome to the show! (Applause) \n Visit us at www.example.com. Thanks! 😉";
        String expected = "Welcome to the show! (Applause) Visit us at www.example.com. Thanks! 😉";
        assertEquals(expected, textCleaner.clean(input));
    }
} 