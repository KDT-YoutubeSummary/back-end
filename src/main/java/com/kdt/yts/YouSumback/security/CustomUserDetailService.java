package com.kdt.yts.YouSumback.security;

import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    // ✅ username 기반 조회 (Spring Security 기본)
    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userName));

        return new CustomUserDetails(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getPasswordHash(),
                null
        );
    }

    // ✅ userId 기반 조회 (JWT 인가 필터에서 사용)
    public UserDetails loadUserByUserId(Long userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        return new CustomUserDetails(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getPasswordHash(),
                null
        );
    }
}
