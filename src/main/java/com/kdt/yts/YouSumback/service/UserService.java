package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.config.JwtProvider;
import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
// UserService는 유저의 로그인 및 회원가입 관련 기능을 처리하는 서비스입니다.
public class UserService {
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    // 로그인 또는 회원가입 처리
    public User loginOrRegister(String email, String name) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .userName(name)
                                .email(email)
                                .passwordHash("GOOGLE") // 👉 소셜 로그인은 비번 없음, 의미 있는 더미 값 지정
                                .build()
                ));
    }

    // 회원가입 처리
    public String issueJwtToken(User user) {
        return jwtProvider.generateToken(user.getId(), user.getEmail());
    }
}

