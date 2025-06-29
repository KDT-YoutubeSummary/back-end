package com.kdt.yts.YouSumback.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
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
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailService customUserDetailService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        log.debug("🔍 JWT Filter 처리 - {} {}", method, requestURI);

        // 인증이 필요 없는 경로들은 JWT 검증을 건너뛰기
        if (isPublicPath(requestURI)) {
            log.debug("✅ Public path detected, skipping JWT validation: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        if (token != null) {
            try {
                if (jwtProvider.validateToken(token)) {
                    Long userId = jwtProvider.extractUserId(token);
                    UserDetails userDetails = customUserDetailService.loadUserByUserId(userId);

                    if (userDetails != null) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("✅ Authentication success for user: {}", userDetails.getUsername());
                    }
                }
            } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
                log.warn("! Invalid JWT token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (ExpiredJwtException e) {
                log.warn("! Expired JWT token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (Exception e) {
                log.error("! JWT token processing failed: {}", e.getMessage(), e);
                SecurityContextHolder.clearContext();
            }
        } else {
            log.debug("🔍 No JWT token found in request: {} {}", method, requestURI);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String requestURI) {
        return requestURI.startsWith("/api/auth/") ||
               requestURI.startsWith("/auth/") ||
               requestURI.startsWith("/oauth2/") ||
               requestURI.startsWith("/login/oauth2/code/") ||
               requestURI.startsWith("/swagger-ui/") ||
               requestURI.startsWith("/v3/api-docs/") ||
               requestURI.startsWith("/actuator/") ||
               requestURI.equals("/error") ||
               requestURI.equals("/") ||
               requestURI.equals("/index.html") ||
               requestURI.matches(".*\\.(ico|png|svg|jpg|jpeg|css|js)$");
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
