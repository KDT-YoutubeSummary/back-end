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

// 추가 import
import jakarta.servlet.http.HttpServletResponse; // HttpServletResponse import
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailService customUserDetailService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }

    @Bean
    public JwtLoginAuthenticationFilter jwtLoginAuthenticationFilter(AuthenticationManager authManager) {
        JwtLoginAuthenticationFilter filter = new JwtLoginAuthenticationFilter(authManager, jwtProvider, userRepository);
        filter.setFilterProcessesUrl("/api/auth/login");
        return filter;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, customUserDetailService);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5174")); // 프론트엔드 주소
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type")); // "Authorization" 헤더 추가
        configuration.setAllowCredentials(true); // 쿠키 허용
        configuration.setMaxAge(3600L); // Pre-flight 캐싱 시간 (Optional)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 적용
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        // URL 권한 설정
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // 인증없이 허용할 경로들
                                "/api/auth/login",            // 로그인
                                "/api/auth/register",         // 회원가입
                                "/oauth2/**",                 // OAuth2 관련 엔드포인트
                                "/swagger-ui/**",             // Swagger UI
                                "/v3/api-docs/**",            // Swagger API 문서
                                "/swagger-resources/**",      // Swagger 리소스
                                "/api-docs/**",               // API 문서 (일반적으로 v3/api-docs 포함)
                                "/webjars/**"                 // Swagger Webjars
                        ).permitAll()
                        .requestMatchers("/api/youtube/upload").authenticated() // 요약 업로드 경로는 인증 필요
                        .anyRequest().authenticated()
        );

        // ✅ 인증/인가 실패 시 처리 방식 정의
        http.exceptionHandling(exceptions -> exceptions
                // 인증되지 않은 사용자가 보호된 리소스에 접근할 때
                .authenticationEntryPoint((request, response, authException) -> {
                    System.err.println("❌ Authentication failed: " + authException.getMessage());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401 응답
                })
                // 인증은 되었지만 권한이 없는 사용자가 접근할 때
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    System.err.println("❌ Access denied: " + accessDeniedException.getMessage());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"); // 403 응답
                })
        );

        // OAuth2 로그인 설정
        http.oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler(oAuth2LoginFailureHandler)
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
        );

        // 기존의 JWT 관련 필터들을 추가합니다.
        http.addFilter(jwtLoginAuthenticationFilter(authManager));
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}