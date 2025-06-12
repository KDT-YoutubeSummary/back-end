package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.QuizAnswerRequest;
import com.kdt.yts.YouSumback.model.dto.request.QuizRequest;
import com.kdt.yts.YouSumback.model.dto.request.QuizRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.QuestionWithOptionsResponse;
import com.kdt.yts.YouSumback.model.dto.response.QuizResultResponse;
import com.kdt.yts.YouSumback.model.entity.Quiz;
import com.kdt.yts.YouSumback.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    // QuizService 대신 SummaryService를 주입합니다.
    private final SummaryService summaryService;

    @PostMapping("/generate")
    public ResponseEntity<List<Quiz>> generateQuiz(@RequestBody QuizRequestDTO request) {
        // SummaryService.generateFromSummary(...)를 호출해야
        // “퀴즈용 프롬프트 → AI 호출 → 파싱 → DB 저장” 로직이 실행됩니다.
        return ResponseEntity.ok(summaryService.generateFromSummary(request));
    }

    @PostMapping("/{quizId}/submit")
    public ResponseEntity<QuizResultResponse> submitQuiz(
            @PathVariable int quizId,
            @RequestBody QuizAnswerRequest request) {
        return ResponseEntity.ok(
                summaryService.checkQuizAnswers(quizId, request.getAnswers())
        );
    }

    @PostMapping("/{quizId}/review")
    public ResponseEntity<List<QuestionWithOptionsResponse>> reviewQuiz(
            @PathVariable int quizId,
            @RequestBody QuizAnswerRequest request) {
        List<QuestionWithOptionsResponse> result =
                summaryService.getQuestionsFromUserAnswers(request.getAnswers());
        return ResponseEntity.ok(result);
    }


}
