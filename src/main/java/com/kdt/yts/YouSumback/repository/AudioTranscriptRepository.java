package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.AudioTranscript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AudioTranscriptRepository extends JpaRepository<AudioTranscript, Long> {
    Optional<AudioTranscript> findByYoutubeId(String youtubeId);

    Optional<AudioTranscript> findByVideoId(Long videoId);

    Optional<AudioTranscript> findByVideo_OriginalUrl(String originalUrl);
}
