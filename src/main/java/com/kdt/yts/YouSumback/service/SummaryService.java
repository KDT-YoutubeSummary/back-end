package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserAnswer;
import com.kdt.yts.YouSumback.model.dto.response.QuestionWithOptionsResponse;
import com.kdt.yts.YouSumback.model.dto.response.QuizResultResponse;
import com.kdt.yts.YouSumback.model.dto.request.QuizRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.model.entity.Quiz;
import com.kdt.yts.YouSumback.model.entity.Summary;
import com.kdt.yts.YouSumback.model.entity.SummaryType;
import com.kdt.yts.YouSumback.model.entity.UserLibrary;

import java.util.List;
import java.util.Optional;


public interface SummaryService {
    SummaryResponseDTO summarize(SummaryRequestDTO request);

    List<Quiz> generateFromSummary(QuizRequestDTO request);

    // 요약 전용 AI 호출 메서드(파싱 로직 전용)
    String callOpenAISummary(String text);

    QuizResultResponse checkQuizAnswers(int quizId, List<UserAnswer> answers);

    List<QuestionWithOptionsResponse> getQuestionsFromUserAnswers(List<UserAnswer> answers);

    Optional<UserLibrary> findUserLibraryByUserAndSummary(Long userId, Summary summary);

}
