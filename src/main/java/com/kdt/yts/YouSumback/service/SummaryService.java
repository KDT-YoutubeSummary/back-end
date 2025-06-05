package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.QuizRequest;
import com.kdt.yts.YouSumback.model.dto.request.SummaryRequest;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponse;
import com.kdt.yts.YouSumback.model.entity.Quiz;

import java.util.List;


public interface SummaryService {
    SummaryResponse summarize(SummaryRequest request);

    // ✅ 새로 추가해야 할 메서드 선언
    List<Quiz> generateFromSummary(QuizRequest request);

    String callOpenAISummary(String text);

}
