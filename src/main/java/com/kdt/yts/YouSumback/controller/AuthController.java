package com.kdt.yts.YouSumback.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kdt.yts.YouSumback.model.dto.request.GoogleLoginRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.LoginRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.RegisterRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.UpdateUserRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.LoginResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.RegisterResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.UpdateUserResponseDTO;
import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.service.GoogleOAuthService;
import com.kdt.yts.YouSumback.service.UserService;
import com.kdt.yts.YouSumback.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
// ⭐️⭐️⭐️ 프론트엔드 호환성을 위해 /api/auth 경로 사용 ⭐️⭐️⭐️
@RequestMapping("/api/auth")
@Tag(name = "인증", description = "사용자 인증 관련 API")
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;
    private final UserService userService;

    // 로그인은 JwtLoginAuthenticationFilter에서 처리됩니다.
    // POST /api/auth/login 요청은 필터가 가로채서 처리합니다.

    // Google 로그인 엔드포인트 (주석 처리된 원본 코드)
    // @Operation(summary = "구글 로그인", description = "Google OAuth를 통한 로그인")
    // @ApiResponses({
    //         @ApiResponse(responseCode = "200", description = "구글 로그인 성공"),
    //         @ApiResponse(responseCode = "401", description = "구글 토큰 인증 실패")
    // })
    // @PostMapping("/google")
    // public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequestDTO request) {
    //     String idToken = request.getId_token();
    //     GoogleIdToken.Payload payload = googleOAuthService.verifyToken(idToken);
    //
    //     String email = payload.getEmail();
    //     String name = (String) payload.get("name");
    //
    //     // fallback 처리
    //     if (name == null || name.isBlank()) {
    //         name = (String) payload.get("given_name");
    //         if (name == null || name.isBlank()) {
    //             name = "GoogleUser";
    //         }
    //     }
    //
    //     User user = userService.loginOrRegister(email, name);
    //     String jwt = userService.issueJwtToken(user);
    //
    //     return ResponseEntity.ok(Map.of("token", jwt));
    // }

    // 회원가입 엔드포인트
    @Operation(summary = "회원가입", description = "새로운 사용자 계정 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 입력 데이터")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        log.info("회원가입 요청 수신 - userName: {}, email: {}", request.getUserName(), request.getEmail());
        
        try {
            // AuthService.register()가 User 엔티티를 반환
            // 생성된 User의 정보를 클라이언트에 돌려주기 위해 RegisterResponse를 생성
            var savedUser = authService.register(request);
            RegisterResponseDTO response = new RegisterResponseDTO(
                    savedUser.getId(),
                    savedUser.getUserName(),
                    savedUser.getEmail(),
                    "회원가입이 성공적으로 완료되었습니다."
            );
            log.info("회원가입 성공 - userId: {}, userName: {}", savedUser.getId(), savedUser.getUserName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("회원가입 실패 - userName: {}, email: {}, error: {}", 
                     request.getUserName(), request.getEmail(), e.getMessage(), e);
            throw e;
        }
    }

    // 회원정보 수정 엔드포인트
    @Operation(summary = "회원정보 수정", description = "기존 사용자 정보 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정보 수정 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PutMapping("/update")
    public ResponseEntity<UpdateUserResponseDTO> updateUser(@RequestBody UpdateUserRequestDTO request) {
        User updated = authService.updateUser(request);
        UpdateUserResponseDTO response = new UpdateUserResponseDTO(
                updated.getId(),
                updated.getUserName(),
                updated.getEmail(),
                "회원정보가 성공적으로 변경되었습니다."
        );
        return ResponseEntity.ok(response);
    }

    // 회원 탈퇴 엔드포인트
    @Operation(summary = "회원 탈퇴", description = "사용자 계정 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "계정 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAccount() {
        authService.deleteUser();
        return ResponseEntity.ok(Collections.singletonMap("message", "회원 탈퇴가 완료되었습니다."));
    }

    // 구글 로그인 시작 엔드포인트
    @Operation(summary = "구글 로그인 시작", description = "구글 OAuth2 로그인 프로세스 시작")
    @GetMapping("/google")
    public ResponseEntity<?> googleLogin() {
        // 실제로는 SecurityConfig에서 처리하므로, 이 URL을 클라이언트에게 알려주는 역할만 합니다.
        String googleLoginUrl = "http://localhost:8080/oauth2/authorization/google";
        return ResponseEntity.ok(Collections.singletonMap("loginUrl", googleLoginUrl));
    }
}