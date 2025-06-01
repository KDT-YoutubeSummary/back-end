// com.YouSumback.model.dto.request.UserRegisterRequestDto
package com.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserRegisterRequestDto {
    private String username;
    private String email;
    private String password;
}
