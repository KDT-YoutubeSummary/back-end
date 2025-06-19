package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.QuizAnswerRequest;
import com.kdt.yts.YouSumback.model.dto.request.QuizRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.UserAnswerDTO;
import com.kdt.yts.YouSumback.model.dto.response.QuestionWithOptionsResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.QuizResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.QuizResultResponseDTO;
import com.kdt.yts.YouSumback.model.entity.Quiz;
import com.kdt.yts.YouSumback.service.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
@Tag(name = "퀴즈", description = "퀴즈 생성 및 제출 관련 API")
public class QuizController {

    private final SummaryService summaryService;

    // 퀴즈 생성
    @Operation(summary = "퀴즈 생성", description = "요약본을 기반으로 새로운 퀴즈를 생성합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "퀴즈 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/generate")
    public ResponseEntity<List<QuizResponseDTO>> generateQuiz(@RequestBody QuizRequestDTO request) {
        // SummaryService.generateFromSummary(...)를 호출해야
        // “퀴즈용 프롬프트 → AI 호출 → 파싱 → DB 저장” 로직이 실행됩니다.
        return ResponseEntity.ok(summaryService.generateFromSummary(request));
    }

    // 퀴즈 제출
    @Operation(summary = "퀴즈 제출", description = "사용자의 퀴즈 답안을 제출하고 결과를 반환합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "퀴즈 제출 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 답안 형식"),
            @ApiResponse(responseCode = "404", description = "퀴즈를 찾을 수 없음")
    })
    @PostMapping("/{quizId}/submit")
    public ResponseEntity<QuizResultResponseDTO> submitQuiz(
            @PathVariable Long quizId,
            @RequestBody QuizAnswerRequest request) {

        // 입력 검증
        for (UserAnswerDTO ans : request.getAnswers()) {
            if (ans.getQuestionId() == null || ans.getAnswerOptionId() == null) {
                throw new IllegalArgumentException("❌ questionId 또는 answerOptionId가 null입니다.");
            }
        }

        return ResponseEntity.ok(
                summaryService.checkQuizAnswers(quizId, request.getAnswers())
        );
    }

    // 퀴즈 리뷰
    @Operation(summary = "퀴즈 리뷰", description = "제출한 퀴즈의 문제와 선택지를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "퀴즈 리뷰 조회 성공"),
            @ApiResponse(responseCode = "404", description = "퀴즈를 찾을 수 없음")
    })
    @PostMapping("/{quizId}/review")
    public ResponseEntity<List<QuestionWithOptionsResponseDTO>> reviewQuiz(
            @PathVariable Long quizId,
            @RequestBody QuizAnswerRequest request) {
        List<QuestionWithOptionsResponseDTO> result =
                summaryService.getQuestionsFromUserAnswers(request.getAnswers());
        return ResponseEntity.ok(result);
    }
}
