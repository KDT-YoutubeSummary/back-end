package com.YouSumback.service;

import com.YouSumback.model.dto.request.LoginRequest;
import com.YouSumback.model.dto.request.RegisterRequest;
import com.YouSumback.model.dto.request.UpdateUserRequest;
import com.YouSumback.model.entity.User;
import com.YouSumback.repository.UserRepository;
import com.YouSumback.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 기존 로그인 메서드
     */
    public String authenticate(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        return jwtProvider.generateToken(user.getUserId(), user.getUsername());
    }

    /**
     * 회원가입 메서드
     */
    public User register(RegisterRequest request) {
        // 1) username/email 중복 체크: UserRepository에 findByUsername, findByEmail 메서드를 구현해야 합니다.
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 사용 중인 사용자명입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }

        // 2) 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3) User 엔티티 생성 및 저장
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(encodedPassword);
        newUser.setCreateAt(LocalDateTime.now());

        try {
            return userRepository.save(newUser);
        } catch (DataIntegrityViolationException e) {
            // 예기치 않은 DB 제약 위반(예: race condition으로 동시에 중복 요청이 들어왔을 때) 처리
            throw new RuntimeException("회원가입 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 회원정보 수정 메서드
     */
    public User updateUser(UpdateUserRequest request) {
        // 1) 현재 인증된 사용자(username) 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // 2) DB에서 User 엔티티 조회
        User existingUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3) 변경하려는 username/email이 다른 사용자와 중복되는지 체크
        if (request.getUsername() != null && !request.getUsername().equals(existingUser.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("이미 사용 중인 사용자명입니다.");
            }
            existingUser.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("이미 사용 중인 이메일입니다.");
            }
            existingUser.setEmail(request.getEmail());
        }

        // 4) 비밀번호 변경(필요 시)
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            // 비밀번호를 평문 그대로 저장하지 않고, BCrypt로 해시
            String encoded = passwordEncoder.encode(request.getPassword());
            existingUser.setPasswordHash(encoded);
        }

        // (선택) 마지막 수정 시각을 기록하려면 추가 필드를 두고 set 해도 됩니다.
        // existingUser.setUpdatedAt(LocalDateTime.now());

        // 5) 저장 및 반환
        try {
            return userRepository.save(existingUser);
        } catch (DataIntegrityViolationException e) {
            // race condition으로 인한 제약 위반 등의 예외 처리
            throw new RuntimeException("회원정보 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 회원정보 삭제 메서드
     */
    public void deleteUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        userRepository.findByUsername(currentUsername)
                .ifPresentOrElse(user -> {
                    userRepository.delete(user);
                    // 만약 “탈퇴 후 로그아웃”을 강제해야 한다면, 여기서 SecurityContextHolder.clearContext()를 호출할 수도 있습니다.
                    SecurityContextHolder.clearContext();
                }, () -> {
                    throw new RuntimeException("존재하지 않는 사용자입니다.");
                });
    }
}
