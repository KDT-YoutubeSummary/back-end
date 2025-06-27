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

    List<SummaryArchive> findByUserId(Long userId);

    Optional<SummaryArchive> findByUserIdAndSummaryId(Long userId, Long summaryId);

    long countByUserId(Long userId);

    @Query("SELECT sa FROM SummaryArchive sa WHERE sa.user.id = :userId ORDER BY sa.lastViewedAt DESC")
    List<SummaryArchive> findRecentlyViewedByUserId(@Param("userId") Long userId, Pageable pageable);

    List<SummaryArchive> findBySummaryId(Long summaryId);
}