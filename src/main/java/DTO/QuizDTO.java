package DTO;

import java.time.LocalDateTime;

public class QuizDTO {

    // 필드
    private long quizId; // 퀴즈 아이디
    private long summaryId; // 요약 아이디
    private String quizTitle; // 퀴즈 제목
    private LocalDateTime createdAt; // 생성 시간

    // 생성자
    public QuizDTO() {}

    // getter
    public long getQuizId() {
        return quizId;
    }
    public long getSummaryId() {
        return summaryId;
    }
    public String getQuizTitle() {
        return quizTitle;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // setter
    public void setQuizId(long quizId) {
        this.quizId = quizId;
    }
    public void setSummaryId(long summaryId) {
        this.summaryId = summaryId;
    }
    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
