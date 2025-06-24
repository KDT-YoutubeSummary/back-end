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
import jakarta.servlet.http.HttpServletResponse;
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

    // PasswordEncoder는 AppConfig에서 정의됨

    @Bean
    public JwtLoginAuthenticationFilter jwtLoginAuthenticationFilter(AuthenticationManager authManager) {
        JwtLoginAuthenticationFilter filter = new JwtLoginAuthenticationFilter(authManager, jwtProvider, userRepository);
        filter.setFilterProcessesUrl("/auth/login");
        return filter;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, customUserDetailService);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // 모든 오리진 허용 (개발용)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*")); // 모든 헤더 허용
        configuration.setAllowCredentials(true); // 쿠키 허용
        configuration.setMaxAge(3600L); // Pre-flight 캐싱 시간 (Optional)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // ✅ JWT만 사용하므로 STATELESS로 변경
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        // URL 권한 설정
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // 인증없이 허용할 경로들
                                "/auth/login",            // 로그인
                                "/auth/register",         // 회원가입
                                "/recommendations/**",    // 추천 API 경로 추가
                                "/oauth2/**",                 // OAuth2 관련 엔드포인트
                                "/login/oauth2/code/**",      // OAuth2 콜백 엔드포인트
                                "/auth/google",           // 구글 로그인 API
                                "/swagger-ui/**",             // Swagger UI
                                "/swagger-ui.html",           // Swagger UI HTML
                                "/swagger-ui/index.html",     // Swagger UI Index
                                "/v3/api-docs/**",            // Swagger API 문서
                                "/v3/api-docs",               // Swagger API 문서 루트
                                "/api-docs/**",               // API 문서
                                "/api-docs",                  // API 문서 루트 (수정됨)
                                "/swagger-resources/**",      // Swagger 리소스
                                "/swagger-config",            // Swagger 설정
                                "/webjars/**",                // Swagger Webjars
                                "/actuator/**",               // Spring Boot Actuator
                                "/error",                     // 에러 페이지
                                "/favicon.ico"               // 파비콘
                        ).permitAll()
                        .requestMatchers("/youtube/upload").authenticated() // 요약 업로드 경로는 인증 필요
                        .requestMatchers("/summary-archives/**").authenticated() // 요약 저장소는 인증 필요
                        .anyRequest().authenticated()
        );

        // ✅ 개선된 인증/인가 실패 처리
        http.exceptionHandling(exceptions -> exceptions
                // 인증되지 않은 사용자가 보호된 리소스에 접근할 때
                .authenticationEntryPoint((request, response, authException) -> {
                    String requestURI = request.getRequestURI();
                    String authHeader = request.getHeader("Authorization");

                    System.err.println("❌ Authentication failed for URI: " + requestURI);
                    System.err.println("❌ Authorization header: " + (authHeader != null ? "Present" : "Missing"));
                    System.err.println("❌ Error: " + authException.getMessage());

                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Authentication required\",\"message\":\"" + authException.getMessage() + "\"}");
                })
                // 인증은 되었지만 권한이 없는 사용자가 접근할 때
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    System.err.println("❌ Access denied: " + accessDeniedException.getMessage());

                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\":\"Access denied\",\"message\":\"" + accessDeniedException.getMessage() + "\"}");
                })
        );

        // OAuth2 로그인 설정
        http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                        .baseUri("/oauth2/authorization") // OAuth2 인증 시작 URI
                )
                .redirectionEndpoint(redirection -> redirection
                        .baseUri("/login/oauth2/code/*") // OAuth2 콜백 URI
                )
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler(oAuth2LoginFailureHandler)
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
        );

        // ✅ JWT 필터 추가 (JwtAuthorizationFilter 제거, JwtAuthenticationFilter만 사용)
        http.addFilter(jwtLoginAuthenticationFilter(authManager));
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}