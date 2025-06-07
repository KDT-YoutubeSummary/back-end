package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SummaryRepository extends JpaRepository<Summary, Integer> {
    Optional<Summary> findByUserUserIdAndAudioTranscriptTranscriptId(Integer userId, Integer transcriptId);

}
