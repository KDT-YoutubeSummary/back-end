package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.dto.response.TagStatResponseDTO;
import com.kdt.yts.YouSumback.model.entity.SummaryArchive;
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

    @Query("SELECT sa FROM SummaryArchive sa JOIN sa.summary s JOIN s.audioTranscript at JOIN at.video v LEFT JOIN sa.tags sat LEFT JOIN sat.tag t WHERE sa.user.id = :userId AND (v.title LIKE %:keyword% OR t.tagName LIKE %:keyword%)")
    List<SummaryArchive> searchByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Query("SELECT new com.kdt.yts.YouSumback.model.dto.response.TagStatResponseDTO(t.tagName, COUNT(t)) FROM SummaryArchive sa JOIN sa.tags sat JOIN sat.tag t WHERE sa.user.id = :userId GROUP BY t.tagName ORDER BY COUNT(t) DESC")
    List<TagStatResponseDTO> findTagUsageStatisticsByUserId(@Param("userId") Long userId);
}