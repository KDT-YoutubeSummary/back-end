package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.AudioTranscript;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioTranscriptRepository extends JpaRepository<AudioTranscript, Long> {
    // Define methods for audio transcript operations if needed
    // For example, findByAudioId, save, delete, etc.
}
