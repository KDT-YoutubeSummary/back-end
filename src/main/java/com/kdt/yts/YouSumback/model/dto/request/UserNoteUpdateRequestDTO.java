package com.kdt.yts.YouSumback.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserNoteUpdateRequestDTO {

    @JsonProperty("user_library_id")
    private Long userLibraryId;

    @JsonProperty("user_notes")
    private String userNotes;
}
