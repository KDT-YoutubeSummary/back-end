package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequestDTO {
    private String userName;    // 변경할 새로운 사용자명
    private String email;       // 변경할 새로운 이메일
    private String password;    // 변경할 새로운 비밀번호 (순수 텍스트)
}
