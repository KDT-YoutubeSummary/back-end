package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserAnswer;
import com.kdt.yts.YouSumback.model.dto.request.QuizRequest;
import com.kdt.yts.YouSumback.model.dto.request.SummaryRequest;
import com.kdt.yts.YouSumback.model.dto.response.QuestionWithOptionsResponse;
import com.kdt.yts.YouSumback.model.dto.response.QuizResultResponse;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponse;
import com.kdt.yts.YouSumback.model.entity.Quiz;

import java.util.List;

public interface SummaryService {

    // 기존 요약 생성 메서드(그대로 두세요)
    SummaryResponse summarize(SummaryRequest request);

    // ✨ 퀴즈 생성용 메서드 선언
    List<Quiz> generateFromSummary(QuizRequest request);

    // 요약 전용 AI 호출 메서드(파싱 로직 전용)
    String callOpenAISummary(String text);

    QuizResultResponse checkQuizAnswers(int quizId, List<UserAnswer> answers);

    List<QuestionWithOptionsResponse> getQuestionsFromUserAnswers(List<UserAnswer> answers);


}
