package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserAnswerDTO;
import com.kdt.yts.YouSumback.model.dto.response.QuestionWithOptionsResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.QuizResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.QuizResultResponseDTO;
import com.kdt.yts.YouSumback.model.dto.request.QuizRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.model.entity.Summary;
import com.kdt.yts.YouSumback.model.entity.SummaryArchive;

import java.util.List;
import java.util.Optional;


public interface SummaryService {
    SummaryResponseDTO summarize(SummaryRequestDTO request, Long userId);

    List<QuizResponseDTO> generateFromSummary(QuizRequestDTO request);

    // 요약 전용 AI 호출 메서드(파싱 로직 전용)
    String callOpenAISummary(String text);

    QuizResultResponseDTO checkQuizAnswers(Long quizId, List<UserAnswerDTO> answers);

    List<QuestionWithOptionsResponseDTO> getQuestionsFromUserAnswers(List<UserAnswerDTO> answers);

    Optional<SummaryArchive> findSummaryArchiveByUserAndSummary(Long userId, Summary summary);

}
