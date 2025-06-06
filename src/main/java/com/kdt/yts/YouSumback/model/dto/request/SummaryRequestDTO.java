package com.kdt.yts.YouSumback.model.dto.request;

import com.kdt.yts.YouSumback.model.entity.SummaryType;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Whisper에서 추출, 정제 후 요약 요청을 위한 DTO
public class SummaryRequestDTO {
        private Long transcriptId;

        private Long userId;

        @NotBlank(message = "요약할 텍스트는 비어 있을 수 없습니다.")
        private String text;

        private String purpose;

        private SummaryType summaryType;
}
//    @NotBlank(message = "요약할 텍스트는 비어 있을 수 없습니다.")
//    private String text;
//
//    private Long transcriptId; // ✅ 추가됨: 어떤 텍스트인지 구분
//    private Long userId;
//
//    @NotBlank(message = "요약 결과는 비어 있을 수 없습니다.")
//    private String summaryText;
//
//    @NotBlank(message = "언어 코드는 필수입니다.")
//    private String languageCode;
//
//    @NotBlank(message = "요약 타입은 필수입니다.")
//    private SummaryType summaryType;
//}
