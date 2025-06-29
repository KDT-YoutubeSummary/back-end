package com.kdt.yts.YouSumback.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        // 인증 관련 요청만 로깅
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/auth/")) {
            log.info("=== 인증 요청 수신 ===");
            log.info("Method: {}", request.getMethod());
            log.info("URI: {}", requestURI);
            log.info("Query String: {}", request.getQueryString());
            log.info("Content-Type: {}", request.getContentType());
            log.info("Origin: {}", request.getHeader("Origin"));
            log.info("User-Agent: {}", request.getHeader("User-Agent"));
            
            // 헤더 정보 로깅
            Collections.list(request.getHeaderNames()).forEach(headerName -> {
                if (headerName.toLowerCase().contains("auth") || 
                    headerName.toLowerCase().contains("content") ||
                    headerName.toLowerCase().contains("accept")) {
                    log.info("Header {}: {}", headerName, request.getHeader(headerName));
                }
            });
            log.info("========================");
        }
        
        filterChain.doFilter(request, response);
        
        // 응답 로깅
        if (requestURI.contains("/auth/")) {
            log.info("=== 인증 응답 ===");
            log.info("Status: {}", response.getStatus());
            log.info("Content-Type: {}", response.getContentType());
            log.info("==================");
        }
    }
} 