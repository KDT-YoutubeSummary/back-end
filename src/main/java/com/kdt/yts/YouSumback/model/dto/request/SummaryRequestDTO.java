package com.kdt.yts.YouSumback.model.dto.request;

import com.kdt.yts.YouSumback.model.entity.SummaryType;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Whisper에서 추출, 정제 후 요약 요청을 위한 DTO
public class SummaryRequestDTO {
        private String originalUrl;
        private String userPrompt; // 사용자 정의 프롬프트 (선택적)
        private SummaryType summaryType = SummaryType.BASIC;

//        private Long transcriptId;
//        private Long userId;
//        @NotBlank(message = "요약할 텍스트는 비어 있을 수 없습니다.")
//        private String text;
}