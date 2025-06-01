package com.YouSumback.repository;

import com.YouSumback.model.entity.TranscriptText;
import com.YouSumback.model.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranscriptRepository extends JpaRepository<TranscriptText, Long> {

    List<TranscriptText> findByVideo(Video video);
    void deleteById(Long transcriptId);
}

