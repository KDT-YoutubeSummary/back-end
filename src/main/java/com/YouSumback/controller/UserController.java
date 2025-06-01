// com.YouSumback.controller.UserController
package com.YouSumback.controller;

import com.YouSumback.model.dto.request.UserRegisterRequestDto;
import com.YouSumback.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserRegisterRequestDto dto) {
        userService.registerUser(dto);
        return ResponseEntity.ok("사용자 등록 완료");
    }
}
