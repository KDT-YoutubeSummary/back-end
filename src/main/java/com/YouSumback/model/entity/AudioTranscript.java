package com.YouSumback.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class AudioTranscript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transcript_id", nullable = false)
    private long transcriptId; // 음성텍스트 식별자

    @Column(name = "transcript_text", columnDefinition = "TEXT", nullable = false)
    private String transcriptText; // 추출된 음성 텍스트

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 생성 일자

    @ManyToOne
    @JoinColumn(name = "video_id", nullable = false)
    private Video video; // 비디오 식별자

    // 음성 식별자 접근자
    public long getTranscriptId() {
        return transcriptId;
    }   // getter

    public void setTranscriptId(long transcriptId) {
        this.transcriptId = transcriptId;
    }   // setter


    // 추출된 음성 텍스트 접근자
    public String getTranscriptText() {
        return transcriptText;
    }   // getter

    public void setTranscriptText(String transcriptText) {
        this.transcriptText = transcriptText;
    }   // setter


    // 생성 일자 접근자
    public LocalDateTime getCreatedAt() {
        return createdAt;
    } // getter

    public void setCreateAt(LocalDateTime createAt) {
        this.createdAt = createAt;
    } // setter


    // 비디오 식별자 접근자
    public Video getVideo() {
        return video;
    }   // getter

    public void setVideo(Video video) {
        this.video = video;
    }   // setter
}
