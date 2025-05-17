package DTO;

public class VideoDTO {

    // 필드
    private String videoId; // 비디오 아이디
    private String title; // 타이틀 제목
    private String originalUrl; // 원본 주소
    private String uploaderName; // 업로드 이름
    private String originalLanguageCode; // 원본 언어 코드
    private int durationSeconds; // 지속 시간

    // 생성자
    public VideoDTO() {}

    // getter
    public String getVideoId() {
        return videoId;
    }
    public String getTitle() {
        return title;
    }
    public String getOriginalUrl() {
        return originalUrl;
    }
    public String getUploaderName() {
        return uploaderName;
    }
    public String getOriginalLanguageCode() {
        return originalLanguageCode;
    }
    public int getDurationSeconds() {
        return durationSeconds;
    }

    // setter
    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }
    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }
    public void setOriginalLanguageCode(String originalLanguageCode) {
        this.originalLanguageCode = originalLanguageCode;
    }
    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

}
