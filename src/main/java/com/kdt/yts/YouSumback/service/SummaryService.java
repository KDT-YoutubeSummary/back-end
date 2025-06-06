package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.QuizRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.model.entity.Quiz;

import java.util.List;


public interface SummaryService {
    SummaryResponseDTO summarize(SummaryRequestDTO request);

    // 새로 추가해야 할 메서드 선언
    List<Quiz> generateFromSummary(QuizRequestDTO request);

    String callOpenAISummary(String text);

    // 요약 생성
    void generateSummary(String youtubeId, String purpose, String summaryType);
}
