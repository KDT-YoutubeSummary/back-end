package com.YouSumback.service;

import com.YouSumback.model.dto.request.QuizRequest;
import com.YouSumback.model.dto.request.SummaryRequest;
import com.YouSumback.model.dto.response.SummaryResponse;
import com.YouSumback.model.entity.Quiz;

import java.util.List;

public interface SummaryService {
    SummaryResponse summarize(SummaryRequest request);

    // ✅ 새로 추가해야 할 메서드 선언
    List<Quiz> generateFromSummary(QuizRequest request);

    String callOpenAISummary(String text);

}
