package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.UserLibraryTag;
import com.kdt.yts.YouSumback.model.entity.UserLibraryTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserLibraryTagRepository extends JpaRepository<UserLibraryTag, UserLibraryTagId> {
    // 특정 라이브러리 id로 연결된 태그 목록 조회
    List<UserLibraryTag> findByUserLibrary_UserLibraryId(Long userLibraryId);
}
