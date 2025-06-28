package com.kdt.yts.YouSumback.security;

import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${yousum.frontend.base-url}")
    private String frontendBaseUrl;

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        log.info("OAuth2 로그인 성공! 핸들러 실행 시작.");

        try {
            // 1. 사용자 정보 추출
            Object principal = authentication.getPrincipal();
            log.info("Principal 타입: {}", principal.getClass().getName());
            
            final String email;
            final String name;
            
            // OAuth2User 또는 OidcUser에서 정보 추출
            if (principal instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) principal;
                email = userPrincipal.getEmail();
                if (userPrincipal.getAttributes() != null) {
                    name = (String) userPrincipal.getAttributes().get("name");
                } else {
                    name = null;
                }
            } else if (principal instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) principal;
                email = oidcUser.getEmail();
                name = oidcUser.getFullName();
                log.info("OidcUser에서 정보 추출: email={}, name={}", email, name);
            } else if (principal instanceof OAuth2User) {
                OAuth2User oAuth2User = (OAuth2User) principal;
                email = (String) oAuth2User.getAttributes().get("email");
                name = (String) oAuth2User.getAttributes().get("name");
                log.info("OAuth2User에서 정보 추출: email={}, name={}", email, name);
            } else {
                throw new RuntimeException("지원하지 않는 Principal 타입: " + principal.getClass().getName());
            }
            
            if (email == null || email.isEmpty()) {
                throw new RuntimeException("이메일 정보를 가져올 수 없습니다.");
            }
            
            log.info("사용자 정보 추출 완료: email={}, name={}", email, name);

            // 2. 사용자 조회 또는 생성
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        log.info("신규 사용자 생성: {}", email);
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setUserName(name != null ? name : "Google User");
                        newUser.setPasswordHash(""); // OAuth 사용자는 비밀번호 없음
                        return userRepository.save(newUser);
                    });

            log.info("사용자 정보 확인 완료: ID={}, Name={}", user.getId(), user.getUserName());

            // 3. JWT 토큰 생성
            String accessToken = jwtProvider.generateToken(user.getId(), user.getUserName());
            log.info("JWT 토큰 생성 성공.");

            // 4. 프론트엔드로 리다이렉트 URL 생성
            String targetUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/oauth/redirect")
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
            // 에러 페이지로 리다이렉트
            String errorUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/login")
                    .queryParam("error", "oauth_processing_failed")
                    .queryParam("message", "OAuth2 processing failed: " + e.getMessage())
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
            response.sendRedirect(errorUrl);
        }
    }
}
