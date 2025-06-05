package com.kdt.yts.YouSumback.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kdt.yts.YouSumback.model.dto.request.GoogleLoginRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.LoginRequest;
import com.kdt.yts.YouSumback.model.dto.request.RegisterRequest;
import com.kdt.yts.YouSumback.model.dto.request.UpdateUserRequest;
import com.kdt.yts.YouSumback.model.dto.response.LoginResponse;
import com.kdt.yts.YouSumback.model.dto.response.RegisterResponse;
import com.kdt.yts.YouSumback.model.dto.response.UpdateUserResponse;
import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.service.GoogleOAuthService;
import com.kdt.yts.YouSumback.service.UserService;
import com.kdt.yts.YouSumback.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@RequiredArgsConstructor
// AuthController는 구글 OAuth 인증을 처리하는 컨트롤러입니다.
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;
    private final UserService userService;

    /**
     * 1) 로그인 엔드포인트 (기존)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String token = authService.authenticate(request);
        return ResponseEntity.ok(new LoginResponse(token));
    }
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequestDTO request) {
        String idToken = request.getId_token();
        System.out.println("👉 받은 id_token: " + idToken); // 디버깅용

    /**
     * 2) 회원가입 엔드포인트 (새로 추가)
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        // AuthService.register()가 User 엔티티를 반환
        // 생성된 User의 정보를 클라이언트에 돌려주기 위해 RegisterResponse를 생성
        var savedUser = authService.register(request);
        RegisterResponse response = new RegisterResponse(
                savedUser.getUserId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                "회원가입이 성공적으로 완료되었습니다."
        );
        return ResponseEntity.ok(response);
    }
        GoogleIdToken.Payload payload = googleOAuthService.verifyToken(idToken);

    /**
     * 회원정보 수정 엔드포인트
     */
    @PutMapping("/update")
    public ResponseEntity<UpdateUserResponse> updateUser(@RequestBody UpdateUserRequest request) {
        User updated = authService.updateUser(request);
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        UpdateUserResponse response = new UpdateUserResponse(
                updated.getUserId(),
                updated.getUsername(),
                updated.getEmail(),
                "회원정보가 성공적으로 변경되었습니다."
        );
        return ResponseEntity.ok(response);
    }
        // fallback 처리 추가
        if (name == null || name.isBlank()) {
            name = (String) payload.get("given_name");  // Google 계정 이름 일부
            if (name == null || name.isBlank()) {
                name = "GoogleUser"; // 완전 없을 경우 기본값
            }
        }

    /**
     * 회원 탈퇴(Delete Account)
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAccount() {
        authService.deleteUser();
        return ResponseEntity.ok()
                .body(Collections.singletonMap("message", "회원 탈퇴가 완료되었습니다."));
        User user = userService.loginOrRegister(email, name);
        String jwt = userService.issueJwtToken(user);

        return ResponseEntity.ok(Map.of("token", jwt));
    }
}
