package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class QuizResponseDTO {
    private String title; // 퀴즈 제목
    private LocalDateTime createdAt; // 퀴즈 생성 시간
    private List<QuestionDTO> questions; // 퀴즈 문제
}

