package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoRegisterRequestDto {
    private String username; // 사용자명
    private String title;    // 영상 제목
    private String videoUrl; // 유튜브 영상 URL
}
