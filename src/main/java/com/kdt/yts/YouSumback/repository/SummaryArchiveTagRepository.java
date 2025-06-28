package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.SummaryArchiveTag;
import com.kdt.yts.YouSumback.model.entity.SummaryArchiveTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SummaryArchiveTagRepository extends JpaRepository<SummaryArchiveTag, SummaryArchiveTagId> {

    // 특정 요약 저장소의 태그 목록 조회
    @Query("SELECT sat FROM SummaryArchiveTag sat JOIN FETCH sat.tag WHERE sat.summaryArchiveId = :summaryArchiveId")
    List<SummaryArchiveTag> findBySummaryArchive_Id(@Param("summaryArchiveId") Long summaryArchiveId);

    // 특정 사용자의 태그별 요약 저장소 개수 조회
    @Query(value = """
        SELECT t.tag_name, COUNT(*) 
        FROM summary_archive sa
        JOIN summary_archive_tag sat ON sa.archive_id = sat.archive_id
        JOIN tag t ON sat.tag_id = t.tag_id
        WHERE sa.user_id = :userId
        GROUP BY t.tag_name
    """, nativeQuery = true)
    List<Object[]> countTagsByUserId(@Param("userId") Long userId);

    // 특정 요약 저장소와 태그 조합 삭제  
    void deleteBySummaryArchive_IdAndTag_Id(Long summaryArchiveId, Long tagId);
}
