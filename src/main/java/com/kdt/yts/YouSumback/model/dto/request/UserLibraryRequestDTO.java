package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class UserLibraryRequestDTO {
    private Long user_id;
    private Long summary_id;
    private String user_notes;

    private List<String> tags; // 태그 ID 목록
}