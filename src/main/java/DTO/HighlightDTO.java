package DTO;


import java.time.LocalDateTime;

public class HighlightDTO {
    // 필드
    private long highlightId; // 하이라이트 아이디
    private long videoId; // 비디오 참조
    private LocalDateTime startTimeSeconds; // 하이라이트 시작 시간
    private LocalDateTime endTimeSeconds; // 하이라이트 끝 시간
    private String highlightDescription; // 하이라이트 설명

    // 생성자
    public HighlightDTO() {}

    //getter
    public long getHighlightId() {
        return highlightId;
    }
    public long getVideoId() {
        return videoId;
    }
    public LocalDateTime getStartTimeSeconds() {
        return startTimeSeconds;
    }
    public LocalDateTime getEndTimeSeconds() {
        return endTimeSeconds;
    }
    public String getHighlightDescription() {
        return highlightDescription;
    }

    //setter
    public void setHighlightId(long highlightId) {
        this.highlightId = highlightId;
    }
    public void setVideoId(long videoId) {
        this.videoId = videoId;
    }
    public void setStartTimeSeconds(LocalDateTime startTimeSeconds) {
        this.startTimeSeconds = startTimeSeconds;
    }
    public void setEndTimeSeconds(LocalDateTime endTimeSeconds) {
        this.endTimeSeconds = endTimeSeconds;
    }
    public void setHighlightDescription(String highlightDescription) {
        this.highlightDescription = highlightDescription;
    }




}
