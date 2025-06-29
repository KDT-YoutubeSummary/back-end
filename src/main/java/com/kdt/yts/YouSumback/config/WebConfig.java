package com.kdt.yts.YouSumback.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// CORS 설정을 위한 클래스 (SecurityConfig에서 처리하므로 비활성화)
// @Configuration
public class WebConfig implements WebMvcConfigurer {

    // @Override
    // public void addCorsMappings(CorsRegistry registry) {
    //     registry.addMapping("/**") // 모든 경로에 대해
    //             .allowedOrigins("http://localhost:5173", "http://www.yousum.site") // 프론트엔드 주소 수정
    //             .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
    //             .allowedHeaders("*") // 모든 헤더 허용으로 변경
    //             .allowCredentials(true); // 필요하면 쿠키도 허용
    // }
}
