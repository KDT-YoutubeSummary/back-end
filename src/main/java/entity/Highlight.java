package entity;

import jakarta.persistence.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Highlight {

    @Id
    @Column(name = "highlight_id", nullable = false)
    private long highlightId; // 하이라이트 아이디

    @ManyToOne
    @JoinColumn(name = "video_id")
    private Video video; // 비디오 참조

    @Column(name = "start_time_seconds", nullable = false)
    private LocalDateTime startTimeSeconds; // 하이라이트 시작 시간

    @Column(name = "end_time_seconds", nullable = false)
    private LocalDateTime endTimeSeconds; // 하이라이트 끝 시간

    @Column(name = "highlight_description", columnDefinition = "TEXT", nullable = true)
    private String highlightDescription; // 하이라이트 설명
}