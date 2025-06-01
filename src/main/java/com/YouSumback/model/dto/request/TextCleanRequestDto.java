package com.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TextCleanRequestDto {
    private String rawText;

    public TextCleanRequestDto() {}

    public TextCleanRequestDto(String rawText) {
        this.rawText = rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
}
