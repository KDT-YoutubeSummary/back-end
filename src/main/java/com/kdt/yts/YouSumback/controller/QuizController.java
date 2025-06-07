package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.QuizRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.QuizSubmitRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.QuizCheckResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.QuizResponseDTO;
import com.kdt.yts.YouSumback.service.QuizService;
import com.kdt.yts.YouSumback.service.QuizServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final QuizServiceImpl quizServiceImpl;

    // ✅ 퀴즈 생성 API
    @PostMapping("/generate")
    public ResponseEntity<?> generateQuiz(@RequestBody QuizRequestDTO request) {
        try {
            List<QuizResponseDTO> result = quizService.generateFromSummary(request);
            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "퀴즈 생성 성공",
                    "data", result
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "퀴즈 생성 실패: " + e.getMessage(),
                    "data", List.of()
            ));
        }
    }

    // ✅ 퀴즈 채점 API
    @PostMapping("/submit")
    public ResponseEntity<?> submitAnswer(@RequestBody QuizSubmitRequestDTO request) {
        try {
            boolean isCorrect = quizServiceImpl.checkAnswer(request.getQuestionId(), request.getSelectedOptionId());
            String explanation = quizServiceImpl.getExplanation(request.getQuestionId());

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "퀴즈 채점 완료",
                    "data", new QuizCheckResponseDTO(isCorrect, explanation)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "채점 실패: " + e.getMessage()
            ));
        }
    }

    // ✅ 퀴즈 요약 ID로 조회 API
    @GetMapping("/summary/{summaryId}")
    public ResponseEntity<?> getQuizBySummary(@PathVariable Long summaryId) {
        try {
            QuizResponseDTO quiz = quizServiceImpl.getQuizBySummaryId(summaryId);
            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "퀴즈 조회 성공",
                    "data", quiz
            ));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "퀴즈 조회 실패: " + e.getMessage()
            ));
        }
    }

    // ✅ 전체 퀴즈 목록 조회 API
    @GetMapping("/list")
    public ResponseEntity<?> getAllQuizList() {
        List<QuizResponseDTO> result = quizServiceImpl.getAllQuizzes();
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "전체 퀴즈 목록",
                "data", result
        ));
    }

    // ✅ 퀴즈 요약 ID로 삭제 API
    @DeleteMapping("/summary/{summaryId}")
    public ResponseEntity<?> deleteQuizBySummary(@PathVariable Long summaryId) {
        try {
            quizServiceImpl.deleteQuizBySummaryId(summaryId);
            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "퀴즈 삭제 완료"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "퀴즈 삭제 실패: " + e.getMessage()
            ));
        }
    }

}