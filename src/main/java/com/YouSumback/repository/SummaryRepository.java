package com.YouSumback.repository;

import com.YouSumback.model.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SummaryRepository extends JpaRepository<Summary, Integer> {
    Optional<Summary> findByUserUserIdAndAudioTranscriptTranscriptId(Long userId, Long transcriptId);

}
