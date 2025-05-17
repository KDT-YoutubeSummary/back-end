package DTO;

import java.time.LocalDateTime;

public class UserLibraryDTO {

    // 필드
    private long userLibraryId; // 유저 라이브러리 아이디
    private String userId; // 유저 아이디 참조
    private String summaryId; // 비디오 아이디 참조
    private LocalDateTime savedAt; // 저장 시간
    private LocalDateTime lastViewedAt; // 마지막 시청 시간
    private String userNotes;  // 유저 노트

    // 생성자
    public UserLibraryDTO() {}

    // getter
    public long getUserLibraryId() {
        return userLibraryId;
    }
    public String getUserId() {
        return userId;
    }
    public String getSummaryId() {
        return summaryId;
    }
    public LocalDateTime getSavedAt() {
        return savedAt;
    }
    public LocalDateTime getLastViewedAt() {
        return lastViewedAt;
    }
    public String getUserNotes() {
        return userNotes;
    }

    // setter
    public void setUserLibraryId(long userLibraryId) {
        this.userLibraryId = userLibraryId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setSummaryId(String summaryId) {
        this.summaryId = summaryId;
    }
    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }
    public void setLastViewedAt(LocalDateTime lastViewedAt) {
        this.lastViewedAt = lastViewedAt;
    }
    public void setUserNotes(String userNotes) {
        this.userNotes = userNotes;
    }
}
