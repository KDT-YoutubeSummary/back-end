package com.YouSumback.controller;

import com.YouSumback.model.dto.request.QuizRequest;
import com.YouSumback.model.entity.Quiz;
import com.YouSumback.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/generate")
    public ResponseEntity<List<Quiz>> generateQuiz(@RequestBody QuizRequest request) {
        return ResponseEntity.ok(quizService.generateFromSummary(request));
    }
}
