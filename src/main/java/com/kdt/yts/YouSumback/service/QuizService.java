package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.QuizRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.QuizResponseDTO;
import com.kdt.yts.YouSumback.model.entity.Quiz;
import jakarta.transaction.Transactional;

import java.util.List;

public interface QuizService {
    List<Quiz> generateFromSummary(QuizRequestDTO request);


//    QuizResponseDTO getQuizBySummaryId(Long summaryId);
}
