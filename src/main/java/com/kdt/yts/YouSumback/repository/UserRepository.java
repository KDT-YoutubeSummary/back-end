package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    // 로그인 시 사용
    Optional<User> findByUsername(String username);

    // 회원가입 중복 체크용
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
