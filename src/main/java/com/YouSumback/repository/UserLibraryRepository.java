package com.YouSumback.repository;

import com.YouSumback.model.entity.UserLibrary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLibraryRepository extends JpaRepository<UserLibrary, Long> {
    // 사용자 ID로 라이브러리 찾기
    UserLibrary findByUserId(Long userId);

    // 라이브러리 삭제
    void deleteById(Long id);
}
