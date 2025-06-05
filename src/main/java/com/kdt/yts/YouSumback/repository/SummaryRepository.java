package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {
    Optional<Summary> findById(Long Id);
    Optional<Summary> findByUserUserIdAndAudioTranscriptTranscriptId(Long userId, Long transcriptId);

}
