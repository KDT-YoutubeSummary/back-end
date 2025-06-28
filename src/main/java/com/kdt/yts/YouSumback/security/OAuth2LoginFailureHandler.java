package com.kdt.yts.YouSumback.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${yousum.frontend.base-url}")
    private String frontendBaseUrl;
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        log.error("소셜 로그인 실패: {}", exception.getMessage());
        String targetUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/login")
                .queryParam("error", "oauth_login_failed")
                .queryParam("message", "OAuth2 authentication failed")
                .build()
                .encode() // URL 인코딩 추가
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
