package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLibraryRepository extends JpaRepository<UserLibrary, Long> {
    Optional<UserLibrary> findBySummaryUserIdAndSummaryAudioTranscriptId(Long userId, Long transcriptId);

    List<UserLibrary> findByUser(User user);

    @Query(value = """
    SELECT t.tag_name, COUNT(*) 
    FROM user_library ul
    JOIN user_library_tag ult ON ul.user_library_id = ult.user_library_id
    JOIN tag t ON ult.tag_id = t.tag_id
    WHERE ul.user_id = :userId
    GROUP BY t.tag_name
""", nativeQuery = true)
    List<Object[]> countTagsById(@Param("userId") Long userId);
}
