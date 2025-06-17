package com.kdt.yts.YouSumback.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 애플리케이션의 핵심 Bean 설정을 담당하는 클래스입니다.
 */
@Configuration
public class AppConfig {

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder Bean을 등록합니다.
     * SecurityConfig에서 분리하여 순환 참조 문제를 해결합니다.
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
