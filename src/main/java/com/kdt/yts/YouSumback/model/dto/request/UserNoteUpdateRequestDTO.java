package com.kdt.yts.YouSumback.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// UserNoteUpdateRequestDTO는 사용자의 메모를 업데이트하기 위한 요청 DTO 클래스입니다.
public class UserNoteUpdateRequestDTO {
    @NotNull(message = "요약 저장소 ID는 필수입니다")
    @JsonProperty("summary_archive_id")
    @JsonAlias({"summaryArchiveId", "summary_archive_id"}) // camelCase와 snake_case 둘 다 지원
    private Long summaryArchiveId;

    @JsonProperty("user_notes")
    @JsonAlias({"userNotes", "user_notes"}) // camelCase와 snake_case 둘 다 지원
    private String note;

    // 기존 메서드명 호환성 유지
    public Long getUserLibraryId() {
        return summaryArchiveId;
    }

    public Long getSummaryArchiveId() {
        return summaryArchiveId;
    }
}
