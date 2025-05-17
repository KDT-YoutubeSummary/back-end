package DTO;

public class AnswerOptionDTO {

    // 필드
    private long answerOptionId;
    private long questionId;
    private String optionText;
    private boolean isCorrect;

    // 기본 생성자
    public AnswerOptionDTO(){}

    // getter
    public long getAnswerOptionId() {
        return answerOptionId;
    }

    public long getQuestionId() {
        return questionId;
    }
    public String getOptionText(){
        return optionText;
    }
    public boolean getIsCorrect(){
        return isCorrect;
    }

    // setter
    public void setAnswerOptionId(long answerOptionId){
       this.answerOptionId = answerOptionId;
    }
    public void setQuestionId(long questionId){
        this.questionId = questionId;
    }
    public void setOptionText(String optionText){
        this.optionText = optionText;
    }
    public void setIsCorrect(boolean isCorrect){
        this.isCorrect = isCorrect;
    }

}
