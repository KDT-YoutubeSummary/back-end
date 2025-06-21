package com.kdt.yts.YouSumback.security;

import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets; // ✨ 1. 인코딩을 위한 charset 임포트

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        log.info("OAuth2 로그인 성공! 핸들러 실행 시작.");

        try {
            // 1. 사용자 정보 추출
            Object principal = authentication.getPrincipal();
            log.info("Principal 타입: {}", principal.getClass().getName());
            
            if (!(principal instanceof UserPrincipal)) {
                throw new RuntimeException("Expected UserPrincipal but got: " + principal.getClass().getName());
            }
            
            UserPrincipal userPrincipal = (UserPrincipal) principal;
            log.info("사용자 정보 추출 완료: {}", userPrincipal.getEmail());

            // 2. 사용자 조회 또는 생성
            User user = userRepository.findByEmail(userPrincipal.getEmail())
                    .orElseGet(() -> {
                        log.info("신규 사용자 생성: {}", userPrincipal.getEmail());
                        User newUser = new User();
                        newUser.setEmail(userPrincipal.getEmail());
                        
                        // OAuth2 attributes에서 사용자 이름 추출
                        String userName = "Google User";
                        if (userPrincipal.getAttributes() != null) {
                            String name = (String) userPrincipal.getAttributes().get("name");
                            if (name != null && !name.isEmpty()) {
                                userName = name;
                            }
                        }
                        newUser.setUserName(userName);
                        newUser.setPasswordHash(""); // OAuth 사용자는 비밀번호 없음
                        return userRepository.save(newUser);
                    });

            log.info("사용자 정보 확인 완료: ID={}, Name={}", user.getId(), user.getUserName());

            // 3. JWT 토큰 생성
            String accessToken = jwtProvider.generateToken(user.getId(), user.getUserName());
            log.info("JWT 토큰 생성 성공.");

            // 4. 프론트엔드로 리다이렉트 URL 생성
            String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth/redirect")
                    .queryParam("accessToken", accessToken)
                    .queryParam("userId", String.valueOf(user.getId()))
                    .queryParam("userName", user.getUserName())
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            log.info("프론트엔드로 리다이렉트할 URL 생성 완료: {}", targetUrl);

            // 5. 리다이렉트 실행
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            log.info("리다이렉트 실행 완료.");

        } catch (Exception e) {
            log.error("OAuth2 로그인 성공 후 처리 중 오류 발생!", e);
            // 에러 페이지로 리다이렉트하지 말고 직접 프론트엔드 에러 페이지로 리다이렉트
            String errorUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/login")
                    .queryParam("error", "oauth_processing_failed")
                    .queryParam("message", "OAuth2 processing failed")
                    .build()
                    .encode() // URL 인코딩 추가
                    .toUriString();
            response.sendRedirect(errorUrl);
        }
    }
}
