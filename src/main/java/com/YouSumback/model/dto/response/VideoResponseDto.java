package com.YouSumback.model.dto.response;

import lombok.Getter;

@Getter
public class VideoResponseDto {


    private String videoId;
    private Long userId;
    private String title;
    private String videoUrl;

    public VideoResponseDto(String videoId, Long userId, String title, String videoUrl) {
        this.videoId = videoId;
        this.userId = userId;
        this.title = title;
        this.videoUrl = videoUrl;
    }

    // getter
}
