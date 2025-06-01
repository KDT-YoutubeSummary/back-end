package com.YouSumback.model.dto.response;

public class CleanedTextResponseDto {
    private String cleanedText;

    public CleanedTextResponseDto(String cleanedText) {
        this.cleanedText = cleanedText;
    }

    public String getCleanedText() {
        return cleanedText;
    }

    public void setCleanedText(String cleanedText) {
        this.cleanedText = cleanedText;
    }

}
