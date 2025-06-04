package com.YouSumback.service;

import com.YouSumback.model.dto.request.QuizRequest;
import com.YouSumback.model.entity.Quiz;

import java.util.List;

public interface QuizService {
    List<Quiz> generateFromSummary(QuizRequest request);
}
