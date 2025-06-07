package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.entity.VideoRecommendation;
import com.kdt.yts.YouSumback.model.dto.response.VideoAiRecommendationResponse;
import com.kdt.yts.YouSumback.service.VideoRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<VideoRecommendation>> aiRecommendAndSave(
            @PathVariable Long userLibraryId
    ) {
        try {
            // 1. Youtube API로 영상 검색 후 AI가 추천 목록 생성
            List<VideoAiRecommendationResponse> recommendationResponses =
                    videoRecommendationService.getAiRecommendationByUserLibraryId(userLibraryId)
                            .block(); // 비동기 -> 동기 처리

            if (recommendationResponses == null || recommendationResponses.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            // 2. 추천 영상과 이유를 DB에 저장
            List<VideoRecommendation> savedRecommendations =
                    videoRecommendationService.saveAiRecommendation(userLibraryId, recommendationResponses);

            if (savedRecommendations.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(savedRecommendations);
        } catch (IllegalArgumentException e) {
            System.err.println("AI 추천 생성 중 오류: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("AI 추천 생성 중 예기치 않은 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

}
