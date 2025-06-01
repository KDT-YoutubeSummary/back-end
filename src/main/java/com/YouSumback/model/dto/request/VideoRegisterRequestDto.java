package com.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VideoRegisterRequestDto {
    private String username;
    private String title;
    private String videoUrl;

    // 생성자, getter, setter
}
