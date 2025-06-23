package com.kdt.yts.YouSumback.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.io.IOException;

public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailService userDetailService;

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager,
                                  JwtProvider jwtProvider,
                                  CustomUserDetailService userDetailService) {
        super(authenticationManager);
        this.jwtProvider = jwtProvider;
        this.userDetailService = userDetailService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        String header = request.getHeader("Authorization");

        // 1️⃣ Authorization 헤더가 아예 없는 경우 → 다음 필터로 넘김
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        // 2️⃣ Bearer 토큰 추출
        String token = header.substring(7);

        try {
            // 3️⃣ 토큰 유효성 검사
            if (jwtProvider.validateToken(token)) {
                Claims claims = jwtProvider.validateAndGetClaims(token);
                Long userId = claims.get("userId", Long.class);

                // 4️⃣ User 조회 및 인증 객체 생성
                CustomUserDetails userDetails =
                        (CustomUserDetails) userDetailService.loadUserByUserId(userId);

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (Exception e) {
            // 5️⃣ 만약 에러나면 인증을 세팅하지 않고 그냥 다음 필터 진행 (익명 처리)
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
