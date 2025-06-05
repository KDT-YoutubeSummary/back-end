package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audio_transcript")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Lob
    @Column(name = "transcript_text", nullable = false, columnDefinition = "LONGTEXT")
    private String transcriptText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
