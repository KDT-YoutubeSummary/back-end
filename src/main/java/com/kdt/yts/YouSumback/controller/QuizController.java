package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.QuizRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.QuizResponseDTO;
import com.kdt.yts.YouSumback.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    // 퀴즈 생성 API 엔드포인트
    @PostMapping("/generate")
    public ResponseEntity<List<QuizResponseDTO>> generateQuiz(@RequestBody QuizRequestDTO request) {
        List<QuizResponseDTO> quizList = quizService.generateFromSummary(request);
        return ResponseEntity.ok(quizList);
    }
}
