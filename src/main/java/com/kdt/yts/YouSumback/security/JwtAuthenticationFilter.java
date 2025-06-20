//package com.kdt.yts.YouSumback.security;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
//@RequiredArgsConstructor
//// JWT 토큰을 검증하고 인증 정보를 설정하는 필터
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//    private final JwtProvider jwtProvider;
//    private final CustomUserDetailService customUserDetailService; // ✅ 변경된 부분
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain)
//            throws ServletException, IOException {
//
//        String path = request.getRequestURI();
//
//        // ✅ 인증 예외 처리할 경로들
//        if (path.startsWith("/api/auth")
//                || path.startsWith("/api-docs/swagger-config")
//                || path.startsWith("/oauth2")
//                || path.startsWith("/swagger-ui")
//                || path.startsWith("/v3/api-docs")
//                || path.startsWith("/swagger-resources")
//                || path.startsWith("/webjars")) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        String token = resolveToken(request);
//
//        if (token != null && jwtProvider.validateToken(token)
//                && SecurityContextHolder.getContext().getAuthentication() == null) {
//
//            try {
//                Long userId = jwtProvider.extractUserId(token);
//                UserDetails userDetails = customUserDetailService.loadUserByUserId(userId);
//
//                UsernamePasswordAuthenticationToken authToken =
//                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//
//                SecurityContextHolder.getContext().setAuthentication(authToken);
//            } catch (Exception e) {
//                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token or user not found");
//                return;
//            }
//        }
//
//        filterChain.doFilter(request, response);
//    }
//
//    private String resolveToken(HttpServletRequest request) {
//        String bearer = request.getHeader("Authorization");
//        if (bearer != null && bearer.startsWith("Bearer ")) {
//            return bearer.substring(7);
//        }
//        return null;
//    }
//}

package com.kdt.yts.YouSumback.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailService customUserDetailService;

    // ✅ 필터 예외 경로 정의
    private static final List<String> EXCLUDE_URLS = Arrays.asList(
            "/api/auth/register",
            "/api/auth/login",
            "/oauth2/",
            "/swagger-ui/",
            "/v3/api-docs/"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDE_URLS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = jwtProvider.resolveToken(request);

        if (token != null && jwtProvider.validateToken(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Long userId = jwtProvider.extractUserId(token);
            UserDetails userDetails = customUserDetailService.loadUserByUserId(userId);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
