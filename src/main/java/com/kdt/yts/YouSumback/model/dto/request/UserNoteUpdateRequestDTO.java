package com.kdt.yts.YouSumback.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// UserNoteUpdateRequestDTO는 사용자의 메모를 업데이트하기 위한 요청 DTO 클래스입니다.
public class UserNoteUpdateRequestDTO {
    @JsonProperty("user_library_id")
    private Long userLibraryId;

    @JsonProperty("user_notes")
    private String userNotes;
}
