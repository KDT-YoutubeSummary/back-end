package com.kdt.yts.YouSumback.service;

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

    // 새로 추가해야 할 메서드 선언
    List<Quiz> generateFromSummary(QuizRequestDTO request);

    String callOpenAISummary(String text);

    Optional<UserLibrary> findUserLibraryByUserAndSummary(Long userId, Summary summary);

    // 요약 생성
    void generateSummary(String youtubeId, String userPrompt, SummaryType summaryType);

}
