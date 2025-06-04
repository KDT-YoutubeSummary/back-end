package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranscriptSaveRequestDto {
    private String url; // URL → 내부에서 youtubeId 추출
}

