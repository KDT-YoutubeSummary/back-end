package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLibraryRepository extends JpaRepository<UserLibrary, Long> {
    // 사용자 ID로 UserLibrary 조회
    List<UserLibrary> findByUserUserId(long userId);

    // 사용자 엔티티로 UserLibrary 조회
    List<UserLibrary> findByUser(User user);

    // 제목 기반 검색
    List<UserLibrary> findBySummary_SummaryTextContaining(String title);

    // 태그 기반 검색
    @Query("SELECT DISTINCT ul FROM UserLibrary ul " +
            "JOIN UserLibraryTag ult ON ult.userLibrary = ul " +
            "JOIN Tag t ON ult.tag = t " +
            "WHERE t.tagName IN :tagNames")
    List<UserLibrary> findByTagNames(@Param("tagNames") List<String> tagNames);
}