package DTO;

import java.time.LocalDateTime;

public class SummaryDTO {
    // 필드
    private long summaryId; // 요약 아이디
    private String videoId; // 비디어 식별자
    private long userLibraryId; // 유저 라이브러리 ID 참조
    private String summaryText; // 요약 텍스트
    private String languageCode; // 언어 코드
    private LocalDateTime createdAt; // 생성 시
    private String  summaryType; // 요약타입

    // 생성자
    public SummaryDTO() {}

    // getter
    public long getSummaryId() {
        return summaryId;
    }
    public String getVideoId(){return videoId;}
    public long getUserLibraryId() {
        return userLibraryId;
    }
    public String getSummaryText() {
        return summaryText;
    }
    public String getLanguageCode() {
        return languageCode;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public String getSummaryType() {
        return summaryType;
    }

    //setter
    public void setSummaryId(long summaryId) {
        this.summaryId = summaryId;
    }
    public void setVideoId(String videoId){this.videoId=videoId;}
    public void setUserLibraryId(long userLibraryId) {
        this.userLibraryId = userLibraryId;
    }
    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }
    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public void setSummaryType(String summaryType) {
        this.summaryType = summaryType;
    }
}
