package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserRegisterRequestDTO;
import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.repository.UserRepository;
import com.kdt.yts.YouSumback.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
// UserService는 유저의 로그인 및 회원가입 관련 기능을 처리하는 서비스입니다.
public class UserService {
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    // 로그인 또는 회원가입 처리
    public User loginOrRegister(String email, String name) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    String generatedUsername = "google_" + UUID.randomUUID().toString().substring(0, 8);

                    // 중복된 userName 피하기 위한 반복 처리
                    while (userRepository.existsByUserName(generatedUsername)) {
                        generatedUsername = "google_" + UUID.randomUUID().toString().substring(0, 8);
                    }

                    return userRepository.save(
                            User.builder()
                                    .userName(generatedUsername)
                                    .email(email)
                                    .passwordHash("GOOGLE") // 소셜 로그인용 더미
                                    .createdAt(LocalDateTime.now())
                                    .build()
                    );
                });
    }

    // 회원가입 처리
    public String issueJwtToken(User user) {
        return jwtProvider.generateToken(user.getId(), user.getEmail());
    }

    public User registerUser(UserRegisterRequestDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        User user = User.builder()
                .userName(dto.getUsername())
                .email(dto.getEmail())
                .passwordHash(dto.getPassword()) // NOTE: 실제 운영 시 해싱 필요
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

}

