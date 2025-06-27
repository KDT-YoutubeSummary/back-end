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

    @Query("SELECT sat FROM SummaryArchiveTag sat JOIN FETCH sat.tag WHERE sat.id.summaryArchiveId = :summaryArchiveId")
    List<SummaryArchiveTag> findBySummaryArchiveId(@Param("summaryArchiveId") Long summaryArchiveId);

    @Query(value = """
        SELECT t.tag_name, COUNT(*) 
        FROM summary_archive sa
        JOIN summary_archive_tag sat ON sa.summary_archive_id = sat.summary_archive_id
        JOIN tag t ON sat.tag_id = t.tag_id
        WHERE sa.user_id = :userId
        GROUP BY t.tag_name
    """, nativeQuery = true)
    List<Object[]> countTagsByUserId(@Param("userId") Long userId);

    void deleteBySummaryArchive_IdAndTag_Id(Long summaryArchiveId, Long tagId);
}
