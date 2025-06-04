package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
// UserLibraryRequestDTO는 사용자가 요약을 라이브러리에 추가할 때 필요한 정보를 담는 DTO입니다.
public class UserLibraryRequestDTO {
    private Long user_id;
    private Long summary_id;
    private String user_notes;

    private List<String> tags; // 태그 ID 목록

}