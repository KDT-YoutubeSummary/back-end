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
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        // ✅ 인증/인가 실패 시 처리 방식 정의 (이 부분 추가 또는 수정)
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


        // URL 권한 설정
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/oauth2/**").permitAll()
                .requestMatchers("/api/youtube/upload").authenticated() // 요약 업로드 경로는 인증 필요
                .anyRequest().authenticated()
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