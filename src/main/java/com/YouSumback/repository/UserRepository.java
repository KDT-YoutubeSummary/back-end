package com.YouSumback.repository;

import com.YouSumback.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 로그인 시 사용
    Optional<User> findByUsername(String username);

    // 이메일 중복 조회
    Optional<User> findByEmail(String email);

    // 회원가입 중복 체크용
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
