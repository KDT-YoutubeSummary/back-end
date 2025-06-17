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
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findByEmail(userPrincipal.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login: " + userPrincipal.getEmail()));

            String accessToken = jwtProvider.generateToken(user.getId(), user.getUserName());
            log.info("JWT 토큰 생성 성공.");

            // ✨ 2. UriComponentsBuilder를 사용하여 URL을 생성하고, UTF-8로 인코딩하도록 명시합니다.
            String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth/redirect")
                    .queryParam("accessToken", accessToken)
                    .queryParam("userId", String.valueOf(user.getId()))
                    .queryParam("userName", user.getUserName()) // 값은 그대로 넣고,
                    .build() // 먼저 URI 구성요소를 빌드한 뒤,
                    .encode(StandardCharsets.UTF_8) // 인코딩을 적용하고,
                    .toUriString(); // 최종 문자열로 변환합니다.

            log.info("프론트엔드로 리다이렉트할 인코딩된 URL 생성 완료: {}", targetUrl);

            // getRedirectStrategy()를 사용하면 스프링 시큐리티가 안전하게 리다이렉트를 처리해줍니다.
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            log.info("리다이렉트 실행 완료.");

        } catch (Exception e) {
            log.error("OAuth2 로그인 성공 후 처리 중 심각한 오류 발생!", e);
            response.sendRedirect("/error?message=OAuth2ProcessError");
        }
    }
}
