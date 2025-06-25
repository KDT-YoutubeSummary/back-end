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
        log.debug("🔍 Request: {} {}", method, path);

        // public path는 토큰 체크 제외
        if (isPublicPath(path)) {
            log.debug("✅ Public path, skip auth: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (token == null) {
            log.warn("⚠️ No JWT token for protected path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!jwtProvider.validateToken(token)) {
                log.warn("❌ Invalid JWT token");
                responseUnauthorized(response, "Invalid or expired token");
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("✅ Already authenticated");
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = jwtProvider.extractUserId(token);
            UserDetails userDetails = customUserDetailService.loadUserByUserId(userId);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("✅ Authentication set for user: {}", userDetails.getUsername());

        } catch (Exception e) {
            log.error("❌ JWT auth failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            responseUnauthorized(response, e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/auth/login")
                || path.startsWith("/auth/register")
                || path.startsWith("/recommendations")
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
            return bearer.substring(7);
        }
        return null;
    }

    private void responseUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return isPublicPath(request.getRequestURI());
    }
}
