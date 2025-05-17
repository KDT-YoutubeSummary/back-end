package DTO;

import java.time.LocalDateTime;

public class VideoRecommendationDTO {

    // 필드
    private long recommendationId; // 영상 추천 식별자
    private String userId; // 추천을 받는 사용자
    private String videoId; // 추천되는 영상
    private String videoId2; // 추천의 계기가 되는 영상
    private String recommendationAiVersion; // 추천을 생성한 AI 모델 버전
    private String recommendationReason; // 추천 이유
    private LocalDateTime createdAt; // 추천 생성 시간
    private boolean isClicked; // 추천영상 클릭 여부
    private LocalDateTime clickedAt; // 클릭 시간


    // 기본 생성자
    public VideoRecommendationDTO() {
    }

    // getter

        public long getRecommendationId() {
        return recommendationId;
        }
        public String getUserId() {
        return userId;
        }
        public String getVideoId() {
        return videoId;
        }
        public String getVideoId2() {
        return videoId2;
        }
        public String getRecommendationAiVersion() {
        return recommendationAiVersion;
        }
        public String getRecommendationReason() {
        return recommendationReason;
        }
        public LocalDateTime getCreatedAt() {
        return createdAt;
        }
        public boolean getIsClicked() {
        return isClicked;
        }
        public LocalDateTime getClickedAt() {
            return clickedAt;
        }
        // setter

        public void setRecommendationId ( long recommendationId){
            this.recommendationId = recommendationId;
        }
        public void setUserId (String userId){
            this.userId = userId;
        }
        public void setVideoId (String videoId) {
            this.videoId = videoId;
        }
        public void setVideoId2 (String videoId2){
            this.videoId2 = videoId2;
        }
        public void setRecommendationAiVersion (String recommendationAiVersion){
            this.recommendationAiVersion = recommendationAiVersion;
        }
        public void setRecommendationReason (String recommendationReason){
            this.recommendationReason = recommendationReason;
        }
        public void setCreatedAt (LocalDateTime createdAt){
            this.createdAt = createdAt;
        }
        public void setIsClicked(boolean isClicked){
        this.isClicked= isClicked;
        }
        public void  setClickedAt(LocalDateTime clickedAt){
        this.clickedAt = clickedAt;
        }

    }

