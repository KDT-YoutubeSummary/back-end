package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.RegisterRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.UserInfoDTO;
import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.repository.UserRepository;
import com.kdt.yts.YouSumback.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder; // ✅ 추가

    // ✅ 일반 로그인 메서드
    public User login(String userName, String plainPassword) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(plainPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }

    // ✅ 구글 로그인 또는 자동 가입
    public User loginOrRegister(String email, String name) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    String generatedUsername = "google_" + UUID.randomUUID().toString().substring(0, 8);

                    while (userRepository.existsByUserName(generatedUsername)) {
                        generatedUsername = "google_" + UUID.randomUUID().toString().substring(0, 8);
                    }

                    return userRepository.save(
                            User.builder()
                                    .userName(generatedUsername)
                                    .email(email)
                                    .passwordHash("GOOGLE")
                                    .createdAt(LocalDateTime.now())
                                    .build()
                    );
                });
    }

    // ✅ 회원가입 시 비밀번호 암호화해서 저장
    public User registerUser(RegisterRequestDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        User user = User.builder()
                .userName(dto.getUserName())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword())) // ✅ 암호화 추가
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public String issueJwtToken(User user) {
        return jwtProvider.generateToken(user.getId(), user.getEmail());
    }

    public UserInfoDTO getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자 없음"));
        return new UserInfoDTO(user.getUserName(), user.getEmail());
    }
}
