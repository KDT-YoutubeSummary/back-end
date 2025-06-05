package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {
    private Long userId;
    private String username;
    private String email;
    private String message; // "회원가입 성공" 같은 간단한 메시지
}
