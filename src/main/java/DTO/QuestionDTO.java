package DTO;

public class QuestionDTO {

    // 필드
    private long questionId; // Question ID
    private long quizId; // 퀴즈 ID 참조
    private String questionText; // 질문 텍스트
    private String languageCode; // 언어 코드

    // 생성자
    public QuestionDTO() {}

    // getter
    public long getQuestionId() {
        return questionId;
    }

    public long getQuizId() {
        return quizId;
    }
    public String getQuestionText() {
        return questionText;
    }
    public String getLanguageCode() {
        return languageCode;
    }

    // setter
    public void setQuestionId(long questionId) {
        this.questionId = questionId;
    }
    public void setQuizId(long quizId) {
        this.quizId = quizId;
    }
    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }
    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

}
