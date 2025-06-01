package com.YouSumback.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TranscriptSaveRequestDto {

    @JsonProperty("video_id")
    private String videoId;

    @JsonProperty("transcriptText")
    private String transcriptText;

    // (생성자/세터가 있다면 그 부분도 확인)
}

