package com.kdt.yts.YouSumback.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class CustomErrorController implements ErrorController {

    @Value("${yousum.frontend.base-url}")
    private String frontendBaseUrl;

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 에러 정보 수집 (표준 속성명 사용)
        Object status = request.getAttribute("jakarta.servlet.error.status_code");
        Object error = request.getAttribute("jakarta.servlet.error.message"); 
        Object exception = request.getAttribute("jakarta.servlet.error.exception");
        String requestUri = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        
        // 기본값 설정
        int statusCode = (status != null) ? (Integer) status : 500;
        String errorMessage = (error != null) ? error.toString() : "Internal Server Error";
        
        log.error("Error occurred - Status: {}, Message: {}, Exception: {}, URI: {}", 
                  statusCode, errorMessage, exception, requestUri);
        
        // API 요청인지 확인 (Accept 헤더나 Content-Type으로 판단)
        String acceptHeader = request.getHeader("Accept");
        boolean isApiRequest = acceptHeader != null && 
                               (acceptHeader.contains("application/json") || 
                                requestUri != null && requestUri.startsWith("/api"));
        
        if (isApiRequest) {
            // API 요청의 경우 JSON 응답 반환
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", true);
            errorResponse.put("status", statusCode);
            errorResponse.put("message", errorMessage);
            errorResponse.put("path", requestUri);
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(statusCode).body(errorResponse);
        } else {
            // 웹 요청의 경우 프론트엔드로 리다이렉트
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/login")
                    .queryParam("error", "oauth_failed")
                    .queryParam("message", "OAuth2 login failed")
                    .build()
                    .encode()
                    .toUriString();
                    
            log.info("Redirecting to frontend with error: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
            return null;
        }
    }
} 