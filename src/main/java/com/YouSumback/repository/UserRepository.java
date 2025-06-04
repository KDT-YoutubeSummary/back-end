package com.YouSumback.repository;

import com.YouSumback.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // 필요 시 커스텀 쿼리 메서드 추가 가능
}
