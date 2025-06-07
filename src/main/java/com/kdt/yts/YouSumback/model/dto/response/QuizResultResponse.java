package com.kdt.yts.YouSumback.model.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class QuizResultResponse {
    private int score;
    private List<Boolean> results;

    public QuizResultResponse(int score, List<Boolean> results) {
        this.score = score;
        this.results = results;
    }
}