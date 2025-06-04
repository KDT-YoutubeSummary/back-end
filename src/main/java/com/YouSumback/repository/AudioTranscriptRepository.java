package com.YouSumback.repository;

import com.YouSumback.model.entity.AudioTranscript;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioTranscriptRepository extends JpaRepository<AudioTranscript, Long> {
    // 필요 시 커스텀 메서드 추가 가능
}
