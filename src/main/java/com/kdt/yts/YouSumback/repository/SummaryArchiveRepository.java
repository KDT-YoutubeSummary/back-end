package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.SummaryArchive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SummaryArchiveRepository extends JpaRepository<SummaryArchive, Long> {

    // 사용자 ID로 요약 저장소 목록 조회
    List<SummaryArchive> findByUserId(Long userId);

    // ⭐️⭐️⭐️ [핵심] ServiceImpl에서 호출하는 메소드 이름과 일치하도록 수정했습니다. ⭐️⭐️⭐️
    // 사용자 ID와 요약 ID로 요약 저장소 조회 (중복 저장 방지)
    Optional<SummaryArchive> findByUserIdAndSummaryId(Long userId, Long summaryId);

    // 사용자의 요약 저장소 개수 조회
    long countByUserId(Long userId);

    // 사용자 ID로 최근 조회한 요약 저장소 목록 (최대 10개)
    @Query("SELECT sa FROM SummaryArchive sa WHERE sa.user.id = :userId ORDER BY sa.lastViewedAt DESC")
    List<SummaryArchive> findRecentlyViewedByUserId(@Param("userId") Long userId, Pageable pageable);

    // 특정 요약을 저장한 모든 사용자의 요약 저장소 조회
    List<SummaryArchive> findBySummaryId(Long summaryId);
}
