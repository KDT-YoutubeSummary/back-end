package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.QuizRequest;
import com.kdt.yts.YouSumback.model.entity.Quiz;

import java.util.List;

public interface QuizService {
    List<Quiz> generateFromSummary(QuizRequest request);
}
