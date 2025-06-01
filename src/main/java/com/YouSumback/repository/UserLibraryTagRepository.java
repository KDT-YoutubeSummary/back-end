package com.YouSumback.repository;

import com.YouSumback.model.entity.UserLibraryTag;
import com.YouSumback.model.entity.UserLibraryTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserLibraryTagRepository extends JpaRepository<UserLibraryTag, UserLibraryTagId> {
    // 특정 라이브러리 id로 연결된 태그 목록 조회
    List<UserLibraryTag> findByUserLibrary_UserLibraryId(Long userLibraryId);
}
