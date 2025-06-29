package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.response.VideoAiRecommendationResponseDTO;
import com.kdt.yts.YouSumback.model.entity.VideoRecommendation;
import com.kdt.yts.YouSumback.service.VideoRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Tag(name = "영상 추천", description = "사용자 맞춤 영상 추천 관련 API")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class VideoRecommendationController {
    @Autowired
    private VideoRecommendationService videoRecommendationService;

    // 영상 추천 테이블에 등록
    @Operation(summary = "추천 영상 등록", description = "영상 추천 테이블에 새로운 추천 영상을 등록합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 영상 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PostMapping
    public VideoRecommendation createRecommendation(
            @RequestBody VideoRecommendation videoRecommendation
    ) {
        return videoRecommendationService.createRecommendation(videoRecommendation);
    }

    // 사용자 ID로 영상 추천 목록 찾기
    @Operation(summary = "사용자별 추천 목록 조회", description = "특정 사용자의 추천 영상 목록을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}")
    public List<VideoRecommendation> getRecommendationsByUserId(
            @PathVariable Long userId
    ) {
        return videoRecommendationService.getRecommendationsByUserId(userId);
    }

    // 사용자 ID로 영상 추천 목록 찾기 (DTO 형식)
    @Operation(summary = "사용자별 추천 목록 조회 (DTO)", description = "특정 사용자의 추천 영상 목록을 DTO 형식으로 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}/dto")
    public List<VideoAiRecommendationResponseDTO> getRecommendationsByUserIdAsDTO(
            @PathVariable Long userId
    ) {
        List<VideoRecommendation> recommendations = videoRecommendationService.getRecommendationsByUserId(userId);
        return videoRecommendationService.toResponseDTO(recommendations);
    }

    // 영상 추천 삭제
    @Operation(summary = "추천 영상 삭제", description = "특정 추천 영상을 삭제합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 영상 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "추천 영상을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<VideoRecommendation> deleteRecommendation(
            @PathVariable Long id
    ) {
        videoRecommendationService.deleteRecommendation(id);
        return ResponseEntity.ok().build();
    }

    // summaryArchiveId 기반 AI 영상 추천 및 저장 (POST)
    @Operation(summary = "AI 영상 추천", description = "요약 저장소 ID를 기반으로 AI가 영상을 추천하고 저장합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI 영상 추천 성공"),
            @ApiResponse(responseCode = "404", description = "요약 저장소를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/ai/{summaryArchiveId}")
    public Mono<ResponseEntity<List<VideoAiRecommendationResponseDTO>>> getAiRecommendation(
            @PathVariable Long summaryArchiveId
    ) {
        log.info("AI 영상 추천 요청 - summaryArchiveId: {}", summaryArchiveId);

        return videoRecommendationService.getAiRecommendationBySummaryArchiveId(summaryArchiveId)
                .map(recommendationResponses -> {
                    log.info("AI 추천 완료 - 추천 개수: {}", recommendationResponses.size());

                    // 추천 결과를 데이터베이스에 저장
                    videoRecommendationService.saveAiRecommendation(summaryArchiveId, recommendationResponses);

                    return ResponseEntity.ok(recommendationResponses);
                })
                .doOnError(error -> log.error("AI 영상 추천 실패 - summaryArchiveId: {}, 오류: {}", summaryArchiveId, error.getMessage()));
    }

    // 주석 처리된 기존 코드도 업데이트
    //    // summaryArchiveId 기반 AI 영상 추천 및 저장 (POST)
    //    @PostMapping("/ai/{summaryArchiveId}")
    //    public ResponseEntity<List<VideoAiRecommendationResponseDTO>> getAiRecommendation(
    //            @PathVariable Long summaryArchiveId
    //    ) {
    //        try {
    //            List<VideoAiRecommendationResponseDTO> recommendationResponses =
    //                    videoRecommendationService.getAiRecommendationBySummaryArchiveId(summaryArchiveId)
    //                            .block(); // 동기적으로 처리
    //
    //            log.info("AI 추천 완료 - 추천 개수: {}", recommendationResponses.size());
    //
    //            // 추천 결과를 데이터베이스에 저장
    //            videoRecommendationService.saveAiRecommendation(summaryArchiveId, recommendationResponses);
    //
    //            return ResponseEntity.ok(recommendationResponses);
    //        } catch (Exception e) {
    //            log.error("AI 영상 추천 실패", e);
    //            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    //        }
    //    }

}
