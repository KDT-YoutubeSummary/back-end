package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserLibraryRequestDTO {
    private Integer user_id;
    private Integer summary_id;
    private String user_notes;
}