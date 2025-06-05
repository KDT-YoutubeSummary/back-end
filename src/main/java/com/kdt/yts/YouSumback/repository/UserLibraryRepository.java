package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLibraryRepository extends JpaRepository<UserLibrary, Integer> {
    // 사용자 ID로 라이브러리 찾기
    UserLibrary findByUser_UserId(Long userId);

    // 라이브러리 삭제
    void deleteById(Integer id);
}
