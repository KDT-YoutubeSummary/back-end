package com.kdt.yts.YouSumback.security;

import com.kdt.yts.YouSumback.model.dto.request.LoginRequestDTO;
import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
// JwtLoginAuthenticationFilter 클래스는 로그인 요청을 처리하는 필터로,
// 사용자가 로그인 시 입력한 자격 증명을 검증하고, 성공 시 JWT 토큰을 생성하여 응답합니다.
// JwtAuthenticationFilter 에서 JwtLoginAuthenticationFilter로 클래스명 수정
public class JwtLoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            LoginRequestDTO loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequestDTO.class);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUserName(),
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

        // ✅ 실제 사용자 엔티티 조회
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtProvider.generateToken(user.getId(), user.getUserName());

        // ✅ JSON 응답 구성
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", token);
        tokenMap.put("username", user.getUserName());
        tokenMap.put("userId", user.getId());

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
