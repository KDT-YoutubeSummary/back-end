package com.YouSumback.controller;

import com.YouSumback.model.entity.User;
import com.YouSumback.model.entity.VideoRecommendation;
import com.YouSumback.service.VideoRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.ArrayList;

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

    /**
     * 라이브러리 ID를 기반으로 관련 태그의 유튜브 영상 추천 받아서 데이터베이스에 저장
     * @param userLibraryId 라이브러리 ID
     * @return 저장된 AI 추천 유튜브 영상 정보 리스트
     */
    @PostMapping("/recommend/{userLibraryId}")
    public Mono<ResponseEntity<List<VideoRecommendation>>> createRecommendationsFromLibrary(
            @PathVariable Long userLibraryId
    ) {
        return videoRecommendationService.getRecommendedVideosByLibraryId(userLibraryId)
                .flatMap(recommendations -> {
                    // 라이브러리 소유자(유저) 조회
                    User user = videoRecommendationService.getUserByLibraryId(userLibraryId);

                    // 각 추천에 유저 설정 및 저장
                    List<VideoRecommendation> savedRecommendations = new ArrayList<>();
                    for (VideoRecommendation recommendation : recommendations) {
                        recommendation.setUser(user);
                        savedRecommendations.add(videoRecommendationService.createRecommendation(recommendation));
                    }

                    return Mono.just(ResponseEntity.ok(savedRecommendations));
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(List.of())));
    }
}
