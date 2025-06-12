package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
// QuizResponseDTO는 퀴즈의 응답을 나타내는 DTO입니다.
public class QuizResponseDTO {
    private Long id; // 퀴즈 ID
    private String title; // 퀴즈 제목
    private LocalDateTime createdAt; // 퀴즈 생성 시간
    private List<QuestionDTO> questions; // 퀴즈 문제
}

