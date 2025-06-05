package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.entity.VideoRecommendation;
import com.kdt.yts.YouSumback.model.dto.response.VideoAiRecommendationResponse;
import com.kdt.yts.YouSumback.service.VideoRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/recommendation")
public class VideoRecommendationController {
    @Autowired
    private VideoRecommendationService videoRecommendationService;

    // 영상 추천 테이블에 등록
    @PostMapping
    public VideoRecommendation createRecommendation(
            @RequestBody VideoRecommendation videoRecommendation
    ) {
        return videoRecommendationService.createRecommendation(videoRecommendation);
    }

    // 사용자 ID로 영상 추천 목록 찾기
    @GetMapping("{userId}")
    public List<VideoRecommendation> getRecommendationsByUserId(
            @PathVariable Long userId
    ) {
        return videoRecommendationService.getRecommendationsByUserId(userId);
    }

    // 영상 추천 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<VideoRecommendation> deleteRecommendation(
            @PathVariable Long id
    ) {
        videoRecommendationService.deleteRecommendation(id);
        return ResponseEntity.ok().build();
    }

    // userLibraryId 기반 AI 영상 추천 및 저장 (POST)
    @PostMapping("/ai/{userLibraryId}")
    public ResponseEntity<Void> aiRecommendAndSave(
            @PathVariable Long userLibraryId
    ) {
        try {
            // OpenAI 응답을 기다리는 동안 202 Accepted 반환
            videoRecommendationService.getAiRecommendationByUserLibraryId(userLibraryId)
                .doOnSuccess(response -> videoRecommendationService.saveAiRecommendation(userLibraryId, response))
                .subscribe(
                    null,
                    error -> {
                        // 예외 발생 시 로깅
                        System.err.println("AI 추천 생성 중 오류 발생: " + error.getMessage());
                        error.printStackTrace();
                    }
                );
            return ResponseEntity.accepted().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
