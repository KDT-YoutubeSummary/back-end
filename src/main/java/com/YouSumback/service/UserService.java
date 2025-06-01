// com.YouSumback.service.UserService
package com.YouSumback.service;

import com.YouSumback.model.dto.request.UserRegisterRequestDto;
import com.YouSumback.model.entity.User;
import com.YouSumback.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public void registerUser(UserRegisterRequestDto dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 username입니다.");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(dto.getPassword());  // 실제 서비스면 비밀번호 해싱해야 함
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
    }
}
