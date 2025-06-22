package com.kdt.yts.YouSumback.controller;

import lombok.extern.slf4j.Slf4j;
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

    @RequestMapping("/error")
    public void handleError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String errorMessage = "OAuth2 login failed";
        
        // 에러 정보 로깅
        Object status = request.getAttribute("javax.servlet.error.status_code");
        Object error = request.getAttribute("javax.servlet.error.message");
        Object exception = request.getAttribute("javax.servlet.error.exception");
        
        log.error("Error occurred - Status: {}, Message: {}, Exception: {}", status, error, exception);
        
        // 프론트엔드로 리다이렉트 (영어 메시지로 변경하여 인코딩 문제 해결)
        String redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/login")
                .queryParam("error", "oauth_failed")
                .queryParam("message", errorMessage)
                .build()
                .encode() // URL 인코딩 추가
                .toUriString();
                
        log.info("Redirecting to frontend with error: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
} 