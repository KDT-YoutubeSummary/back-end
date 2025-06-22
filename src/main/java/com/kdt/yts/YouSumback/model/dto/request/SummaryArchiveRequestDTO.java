package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
// SummaryArchiveRequestDTO는 사용자가 요약을 아카이브에 추가할 때 필요한 정보를 담는 DTO입니다.
public class SummaryArchiveRequestDTO {
    // 사용자 ID는 인증 정보에서 가져오므로 DTO에는 포함하지 않습니다.
    private Long summaryId;
    private String userNotes;
    private List<String> tags; // 태그 목록
}