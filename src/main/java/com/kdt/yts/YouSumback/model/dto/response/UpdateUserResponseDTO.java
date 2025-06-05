package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateUserResponseDTO {
    private Long userId;
    private String userName;
    private String email;
    private String message;   // 예: "회원정보가 성공적으로 변경되었습니다."
}
