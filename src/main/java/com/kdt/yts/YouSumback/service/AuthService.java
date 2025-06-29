package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.LoginRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.RegisterRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.UpdateUserRequestDTO;
import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.repository.UserRepository;
import com.kdt.yts.YouSumback.security.CustomUserDetails;
import com.kdt.yts.YouSumback.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.kdt.yts.YouSumback.exception.UserAlreadyExistsException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
// 사용자 인증, 회원가입, 회원정보 수정 및 삭제 기능을 제공하는 서비스 클래스입니다.
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // 회원가입 메서드
    public User register(RegisterRequestDTO request) {
        // 1) username/email 중복 체크: UserRepository에 findByUsername, findByEmail 메서드를 구현해야 합니다.
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new UserAlreadyExistsException("이미 사용 중인 사용자명입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("이미 사용 중인 이메일입니다.");
        }

        // 2) 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3) User 엔티티 생성 및 저장
        User newUser = new User();
        newUser.setUserName(request.getUserName());
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(encodedPassword);
        newUser.setCreatedAt(LocalDateTime.now());

        try {
            return userRepository.save(newUser);
        } catch (DataIntegrityViolationException e) {
            // 예기치 않은 DB 제약 위반(예: race condition으로 동시에 중복 요청이 들어왔을 때) 처리
            throw new UserAlreadyExistsException("회원가입 처리 중 오류가 발생했습니다.");
        }
    }

    // 회원정보 수정
    public User updateUser(UpdateUserRequestDTO request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new RuntimeException("인증된 사용자 정보가 올바르지 않습니다.");
        }

        Long userId = userDetails.getUserId();

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getUserName() != null && !request.getUserName().equals(existingUser.getUserName())) {
            if (userRepository.existsByUserName(request.getUserName())) {
                throw new RuntimeException("이미 사용 중인 사용자명입니다.");
            }
            existingUser.setUserName(request.getUserName());
        }

        if (request.getEmail() != null && !request.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("이미 사용 중인 이메일입니다.");
            }
            existingUser.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            String encoded = passwordEncoder.encode(request.getPassword());
            existingUser.setPasswordHash(encoded);
        }

        try {
            return userRepository.save(existingUser);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("회원정보 수정 중 오류가 발생했습니다.", e);
        }
    }

    // 회원 탈퇴
    public void deleteUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();

        userRepository.findById(userId)
                .ifPresentOrElse(user -> {
                    userRepository.delete(user);
                    SecurityContextHolder.clearContext();
                }, () -> {
                    throw new RuntimeException("존재하지 않는 사용자입니다.");
                });
    }
}
