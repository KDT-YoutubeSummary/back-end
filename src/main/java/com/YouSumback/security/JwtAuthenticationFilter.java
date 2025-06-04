package com.YouSumback.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.YouSumback.model.dto.request.LoginRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtProvider jwtProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        // 로그인 URL을 "/api/auth/login"으로 변경하고 싶다면 설정
        setFilterProcessesUrl("/api/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    );

            return authenticationManager.authenticate(authToken);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {

        org.springframework.security.core.userdetails.User principal =
                (org.springframework.security.core.userdetails.User) authResult.getPrincipal();

        String token = jwtProvider.generateToken(
                null,
                /* userId: 필요 시 null 대신 실제 ID를 넣어주세요 */
                principal.getUsername()
        );

        response.addHeader("Authorization", "Bearer " + token);

        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", token);

        response.setContentType("application/json; charset=UTF-8");
        new ObjectMapper().writeValue(response.getOutputStream(), tokenMap);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=UTF-8");

        Map<String, String> error = new HashMap<>();
        error.put("error", "Authentication failed: " + failed.getMessage());
        new ObjectMapper().writeValue(response.getOutputStream(), error);
    }
}
