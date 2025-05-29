package com.YouSumback.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SummaryRequest {
    @NotBlank(message = "요약할 텍스트는 비어 있을 수 없습니다.")
    private String text;

    private Long transcriptId; // ✅ 추가됨: 어떤 텍스트인지 구분
}
