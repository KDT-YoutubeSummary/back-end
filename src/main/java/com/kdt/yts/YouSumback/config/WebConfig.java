//package com.kdt.yts.YouSumback.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//// CORS 설정을 위한 클래스
//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**") // 모든 경로에 대해
//                .allowedOrigins("http://localhost:5174") // 프론트엔드 주소
//                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
////                .allowedHeaders("*")
//                .allowedHeaders("Authorization", "Content-Type")
//                .allowCredentials(true); // 필요하면 쿠키도 허용
//    }
//}
