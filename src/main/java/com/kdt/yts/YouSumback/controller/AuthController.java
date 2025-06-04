package com.kdt.yts.YouSumback.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kdt.yts.YouSumback.model.dto.request.GoogleLoginRequestDTO;
import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.service.GoogleOAuthService;
import com.kdt.yts.YouSumback.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
// AuthController는 구글 OAuth 인증을 처리하는 컨트롤러입니다.
public class AuthController {

    private final GoogleOAuthService googleOAuthService;
    private final UserService userService;

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequestDTO request) {
        String idToken = request.getId_token();
        System.out.println("👉 받은 id_token: " + idToken); // 디버깅용

        GoogleIdToken.Payload payload = googleOAuthService.verifyToken(idToken);

        String email = payload.getEmail();
        String name = (String) payload.get("name");

        // fallback 처리 추가
        if (name == null || name.isBlank()) {
            name = (String) payload.get("given_name");  // Google 계정 이름 일부
            if (name == null || name.isBlank()) {
                name = "GoogleUser"; // 완전 없을 경우 기본값
            }
        }

        User user = userService.loginOrRegister(email, name);
        String jwt = userService.issueJwtToken(user);

        return ResponseEntity.ok(Map.of("token", jwt));
    }
}
