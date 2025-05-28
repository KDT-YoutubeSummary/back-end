package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLibraryRepository {
    List<UserLibrary> findByUser(User testUser);
}
