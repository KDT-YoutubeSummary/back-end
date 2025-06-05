package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponseDTO {
    private Long userId;
    private String userName;
    private String email;
    private String message; // "회원가입 성공" 같은 간단한 메시지
}
