package com.kdt.yts.YouSumback.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SummaryRequest {
    @NotBlank(message = "요약할 텍스트는 비어 있을 수 없습니다.")
    private String text;

    // ↓ PK 타입과 맞추기 위해 Long → Integer 로 변경합니다.
    private Integer transcriptId;
    private Integer userId;

    @NotBlank(message = "언어 코드는 필수입니다.")
    private String languageCode;

    // SummaryResponse에서는 이미 summaryId와 summaryText만 반환하므로
    // DTO에 summaryText 필드는 불필요할 수 있습니다.
    // private String summaryText;  // (필요 없으면 제거)
}
