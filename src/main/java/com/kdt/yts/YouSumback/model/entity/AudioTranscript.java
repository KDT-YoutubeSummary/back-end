package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audiotranscript")    // 실제 MySQL 테이블명과 반드시 일치
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioTranscript {

    @Id
    @Column(name = "transcript_id", nullable = false)
    private Integer transcriptId;

    @Column(name = "transcript_text", columnDefinition = "TEXT", nullable = false)
    private String transcriptText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToOne
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;
}
