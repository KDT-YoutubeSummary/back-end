package com.YouSumback.repository;

import com.YouSumback.model.entity.UserLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLibraryRepository extends JpaRepository<UserLibrary, Long> {
    Optional<UserLibrary> findBySummaryUserUserIdAndSummaryAudioTranscriptTranscriptId(Long userId, Long transcriptId);

}
