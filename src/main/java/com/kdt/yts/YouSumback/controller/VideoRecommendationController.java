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
    public ResponseEntity<List<VideoRecommendation>> aiRecommendAndSave(
            @PathVariable Long userLibraryId
    ) {
        try {
            List<VideoRecommendation> savedList = new java.util.ArrayList<>();
            videoRecommendationService.getAiRecommendationByUserLibraryId(userLibraryId)
                .doOnSuccess(responseList -> {
                    List<VideoRecommendation> result = videoRecommendationService.saveAiRecommendation(userLibraryId, responseList);
                    savedList.addAll(result);
                    result.forEach(r -> System.out.println("저장된 추천: " + r));
                })
                .block(); // 비동기 -> 동기 처리
            return ResponseEntity.ok(savedList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
