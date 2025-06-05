package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
//    Optional<User> findById(Long id);
    Optional<User> findByUserName(String username);
    Optional<User> findByEmail(String email);

    // 로그인 시 사용
    Optional<User> findByUsername(String username);

    // 회원가입 중복 체크용
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
