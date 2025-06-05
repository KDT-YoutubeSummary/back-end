package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VideoRegisterResponseDTO {
    private Long videoId;
    private String username;
    private String title;
    private String videoUrl;
}
