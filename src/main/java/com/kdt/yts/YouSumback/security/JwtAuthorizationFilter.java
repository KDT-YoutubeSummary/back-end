package com.kdt.yts.YouSumback.security;

import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// JwtAuthorizationFilter 클래스는 JWT 토큰을 검증하고,
// 유효한 경우 인증 정보를 SecurityContext에 설정하는 필터입니다.
// JwtAuthenticationFilter 하나로 인증과 인가를 모두 처리할 수 있긴 함
public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, JwtProvider jwtProvider, UserRepository userRepository) {
        super(authenticationManager);
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.replace("Bearer ", "");
        try {
            Claims claims = jwtProvider.validateAndGetClaims(token);
            if (claims != null) {
                Long userId = claims.get("userId", Long.class); // ✅ userId도 claims에 들어있어야 함
                String username = claims.getSubject();

                // DB에서 유저 정보 조회 후 CustomUserDetails로 감싸기
                User user = userRepository.findByUserName(username)
                        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

                CustomUserDetails userDetails = new CustomUserDetails(user);

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (Exception e) {
            // 유효하지 않은 토큰이면 인증 정보 제거
            SecurityContextHolder.clearContext();
            // 선택: 로그 남기기, response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        chain.doFilter(request, response);
    }



}
