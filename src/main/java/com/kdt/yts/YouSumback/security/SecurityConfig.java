package com.kdt.yts.YouSumback.security;

import com.kdt.yts.YouSumback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
// Spring Security 설정을 위한 클래스입니다. JWT 기반 인증을 사용하며, Google 로그인과 일반 로그인 API를 허용합니다.
// SecurityConfig.java (Spring Security 6+ 함수형 DSL)
public class SecurityConfig {

    private final CustomUserDetailService userDetailService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 로그인 요청 시 필터 → 아이디/비번 인증 + JWT 발급
    @Bean
    public JwtLoginAuthenticationFilter jwtLoginAuthenticationFilter(AuthenticationManager authManager) {
        JwtLoginAuthenticationFilter filter = new JwtLoginAuthenticationFilter(authManager, jwtProvider, userRepository);
        filter.setFilterProcessesUrl("/api/auth/login");
        return filter;
    }

    // 요청마다 JWT 토큰 검사 → SecurityContext 인증
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, userRepository);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {}) // CORS 허용 (개발용)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/google"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilter(jwtLoginAuthenticationFilter(authManager)) // 로그인 필터
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class) // JWT 인증 필터
                .addFilterBefore(new JwtAuthorizationFilter(authManager, jwtProvider), UsernamePasswordAuthenticationFilter.class) // (선택) 권한 필터
                .formLogin(form -> form.disable())
                .build();
    }
}

//    // AuthenticationManager를 Bean으로 등록 (Spring Security 6 방식)
//    @Bean
//    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
//        // AuthenticationManagerBuilder를 직접 가져와서 UserDetailsService와 PasswordEncoder를 연결
//        AuthenticationManagerBuilder authBuilder =
//                http.getSharedObject(AuthenticationManagerBuilder.class);
//
//        authBuilder
//                .userDetailsService(userDetailService)
//                .passwordEncoder(passwordEncoder());
//
//        return authBuilder.build();
//    }
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        AuthenticationManager authManager = authenticationManager(http);
//
//        JwtAuthenticationFilter jwtAuthFilter =
//                new JwtAuthenticationFilter(authManager, jwtProvider, userRepository);
//
//        jwtAuthFilter.setFilterProcessesUrl("/api/auth/login");
//
//        JwtAuthorizationFilter jwtAuthzFilter =
//                new JwtAuthorizationFilter(authManager, jwtProvider);
//
//        http
//                .csrf(csrf -> csrf.disable())
//                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(authz -> authz
//                        // 로그인/회원가입은 모두 허용
//                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
//                        // 나머지 모든 요청은 인증 필요
//                .cors(Customizer.withDefaults()) // 개발 환경용 CORS 허용
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/auth/google", "/api/auth/login").permitAll() // 로그인 관련 허용
//                        .anyRequest().authenticated()
//                )
//                .authenticationManager(authManager)
//                .addFilter(jwtAuthFilter)
//                .addFilterBefore(jwtAuthzFilter, UsernamePasswordAuthenticationFilter.class);
//                .formLogin(form -> form.disable())
//                .addFilterBefore(
//                        jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class
//                );
//
//        return http.build();
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    public Filter jwtAuthenticationFilter() {
//        return new JwtAuthenticationFilter(jwtProvider, userRepository);
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
//        return config.getAuthenticationManager();
//    }
//}
