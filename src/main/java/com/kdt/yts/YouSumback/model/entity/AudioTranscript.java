package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audio_transcript")
public class AudioTranscript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transcript_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "video_id", referencedColumnName = "video_id", nullable = false, unique = true)
    private Video video;

    @Column(name = "youtube_id", nullable = false, length = 100)
    private String youtubeId;

    // S3 파일 경로 (기존 방식 - 필요시 사용)
    @Lob
    @Column(name = "transcript_path", columnDefinition = "LONGTEXT")
    private String transcriptPath;

    // DB에 직접 텍스트 저장 (새로운 방식)
    @Lob
    @Column(name = "transcript_text", columnDefinition = "LONGTEXT")
    private String transcriptText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
