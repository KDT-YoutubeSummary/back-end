package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLibraryRepository extends JpaRepository<UserLibrary, Long> {
    Optional<UserLibrary> findBySummaryUserUserIdAndSummaryAudioTranscriptTranscriptId(Long userId, Long transcriptId);

    List<UserLibrary> findByUser(User user);

    List<Object[]> countTagsById(Long userId);
}
