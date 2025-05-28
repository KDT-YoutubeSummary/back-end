package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository {
    User save(User user);
}
