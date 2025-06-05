package com.kdt.yts.YouSumback.config;

import com.kdt.yts.YouSumback.repository.UserRepository;
import com.kdt.yts.YouSumback.security.JwtAuthenticationFilter;
import com.kdt.yts.YouSumback.security.JwtAuthorizationFilter;
import com.kdt.yts.YouSumback.security.JwtProvider;

import com.kdt.yts.YouSumback.security.CustomUserDetailService; // ← 패키지 경로 수정
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder; // ← import 추가
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// SecurityConfig.java (Spring Security 6+ 함수형 DSL)
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
// Spring Security 설정을 위한 클래스입니다. JWT 기반 인증을 사용하며, Google 로그인과 일반 로그인 API를 허용합니다.
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailService userDetailService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        // AuthenticationManagerBuilder를 직접 가져와서 UserDetailsService와 PasswordEncoder를 연결
        AuthenticationManagerBuilder authBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authBuilder
                .userDetailsService(userDetailService)
                .passwordEncoder(passwordEncoder());

        return authBuilder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AuthenticationManager authManager = authenticationManager(http);

        JwtAuthenticationFilter jwtAuthFilter =
                new JwtAuthenticationFilter(authManager, jwtProvider, userRepository);

        jwtAuthFilter.setFilterProcessesUrl("/api/auth/login");

        JwtAuthorizationFilter jwtAuthzFilter =
                new JwtAuthorizationFilter(authManager, jwtProvider);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // 로그인/회원가입은 모두 허용
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        // 나머지 모든 요청은 인증 필요
                .cors(Customizer.withDefaults()) // 개발 환경용 CORS 허용
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/google", "/api/auth/login").permitAll() // 로그인 관련 허용
                        .anyRequest().authenticated()
                )
                .authenticationManager(authManager)
                .addFilter(jwtAuthFilter)
                .addFilterBefore(jwtAuthzFilter, UsernamePasswordAuthenticationFilter.class);
                .formLogin(form -> form.disable())
                .addFilterBefore(
                        jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    public Filter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, userRepository);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
