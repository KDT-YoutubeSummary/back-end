// 수정 후: src/main/java/com/YouSumback/security/CustomUserDetailService.java
package com.kdt.yts.YouSumback.security;

import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
// UserDetailsService 인터페이스를 구현하여 사용자 인증 정보를 로드
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    // Spring Security 기본 로그인 흐름에 사용
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");

        return new CustomUserDetails(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getPasswordHash(),
                Collections.singleton(authority)
        );
    }

    // JWT 토큰에서 userId로 사용자 조회할 때 사용
    public UserDetails loadUserByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found by ID: " + userId));

        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");

        return new CustomUserDetails(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getPasswordHash(),
                Collections.singleton(authority)
        );
    }
}
        //    @Override
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        User user = userRepository.findByUserName(username)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
//        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
//
//        return org.springframework.security.core.userdetails.User.builder()
//                .username(user.getUserName())
//                .password(user.getPasswordHash())
//                .authorities(Collections.singleton(authority))
//                .build();
//    }

