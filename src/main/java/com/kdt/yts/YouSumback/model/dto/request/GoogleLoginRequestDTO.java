package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
// Google 로그인 요청을 위한 DTO 클래스
public class GoogleLoginRequestDTO {
    private String id_token;

}
