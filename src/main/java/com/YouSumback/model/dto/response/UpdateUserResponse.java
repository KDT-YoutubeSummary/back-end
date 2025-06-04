package com.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateUserResponse {
    private Long userId;
    private String username;
    private String email;
    private String message;   // 예: "회원정보가 성공적으로 변경되었습니다."
}
