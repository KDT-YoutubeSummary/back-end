package com.kdt.yts.YouSumback.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
// JWT 토큰을 검증하고 인증 정보를 설정하는 필터
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailService customUserDetailService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // ✅ 디버깅을 위한 요청 정보 로깅
        log.debug("🔍 Processing request: {} {}", method, path);

        // ✅ 인증 예외 처리할 경로들
        if (isPublicPath(path)) {
            log.debug("✅ Public path, skipping authentication: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ Authorization 헤더 검사
        String authHeader = request.getHeader("Authorization");
        log.debug("🔑 Authorization header: {}", authHeader != null ? "Present" : "Missing");

        String token = resolveToken(request);

        if (token == null) {
            log.warn("⚠️ No JWT token found in request for protected path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            log.debug("🔍 Validating JWT token...");
            
            if (!jwtProvider.validateToken(token)) {
                log.warn("❌ Invalid JWT token for path: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            // ✅ 이미 인증된 경우 스킵
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("✅ Already authenticated, skipping");
                filterChain.doFilter(request, response);
                return;
            }

            // ✅ 사용자 정보 로딩 및 인증 설정
            Long userId = jwtProvider.extractUserId(token);
            log.debug("🆔 Extracted userId from token: {}", userId);

            UserDetails userDetails = customUserDetailService.loadUserByUserId(userId);
            log.debug("👤 Loaded user details for: {}", userDetails.getUsername());

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("✅ Authentication set successfully for user: {}", userDetails.getUsername());

        } catch (Exception e) {
            log.error("❌ JWT authentication failed for path {}: {}", path, e.getMessage());
            SecurityContextHolder.clearContext();
            
            // ✅ 인증 실패 시 401 응답 반환
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid or expired token\",\"message\":\"" + e.getMessage() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/recommendations")  
                || path.startsWith("/api-docs/swagger-config")
                || path.startsWith("/oauth2")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")
                || path.startsWith("/error")
                || path.equals("/favicon.ico");
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7);
            log.debug("🎫 Extracted token: {}...", token.length() > 10 ? token.substring(0, 10) : token);
            return token;
        }
        return null;
    }
}
