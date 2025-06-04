package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.AudioTranscript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AudioTranscriptRepository extends JpaRepository<AudioTranscript, Long> {
    List<AudioTranscript> findByYoutubeId(String youtubeId);
}
