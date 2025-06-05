package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
//    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);

    // 로그인 시 사용
    Optional<User> findByUserName(String userName);

    // 회원가입 중복 체크용
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);
}
