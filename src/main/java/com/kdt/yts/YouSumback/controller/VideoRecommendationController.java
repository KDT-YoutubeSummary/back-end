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

import java.util.List;

@RestController
@RequestMapping("/api/recommendation")
@Tag(name = "영상 추천", description = "영상 추천 관리 API")
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
    @GetMapping("{userId}")
    public List<VideoRecommendation> getRecommendationsByUserId(
            @PathVariable Long userId
    ) {
        return videoRecommendationService.getRecommendationsByUserId(userId);
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

    // userLibraryId 기반 AI 영상 추천 및 저장 (POST)
    @Operation(summary = "AI 영상 추천", description = "사용자 라이브러리 ID를 기반으로 AI가 영상을 추천하고 저장합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 추천 성공"),
            @ApiResponse(responseCode = "204", description = "추천할 컨텐츠 없음"),
            @ApiResponse(responseCode = "404", description = "라이브러리를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/ai/{userLibraryId}")
    public ResponseEntity<List<VideoAiRecommendationResponseDTO>> aiRecommendAndSave(
            @PathVariable Long userLibraryId
    ) {
        try {
            // 1. Youtube API로 영상 검색 후 AI가 추천 목록 생성
            List<VideoAiRecommendationResponseDTO> recommendationResponses =
                    videoRecommendationService.getAiRecommendationByUserLibraryId(userLibraryId)
                            .block(); // 비동기 → 동기 처리

            if (recommendationResponses == null || recommendationResponses.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            // 2. 추천 영상과 이유를 DB에 저장
            List<VideoRecommendation> savedRecommendations =
                    videoRecommendationService.saveAiRecommendation(userLibraryId, recommendationResponses);

            if (savedRecommendations.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            // 3. 저장된 엔티티를 DTO로 변환하여 반환
            List<VideoAiRecommendationResponseDTO> responseDTOs =
                    videoRecommendationService.toResponseDTO(savedRecommendations);

            return ResponseEntity.ok(responseDTOs);

        } catch (IllegalArgumentException e) {
            System.err.println("AI 추천 생성 중 오류: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("AI 추천 생성 중 예기치 않은 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

//    // userLibraryId 기반 AI 영상 추천 및 저장 (POST)
//    @PostMapping("/ai/{userLibraryId}")
//    public ResponseEntity<List<VideoRecommendation>> aiRecommendAndSave(
//            @PathVariable Long userLibraryId
//    ) {
//        try {
//            // 1. Youtube API로 영상 검색 후 AI가 추천 목록 생성
//            List<VideoAiRecommendationResponseDTO> recommendationResponses =
//                    videoRecommendationService.getAiRecommendationByUserLibraryId(userLibraryId)
//                            .block(); // 비동기 -> 동기 처리
//
//            if (recommendationResponses == null || recommendationResponses.isEmpty()) {
//                return ResponseEntity.noContent().build();
//            }
//
//            // 2. 추천 영상과 이유를 DB에 저장
//            List<VideoRecommendation> savedRecommendations =
//                    videoRecommendationService.saveAiRecommendation(userLibraryId, recommendationResponses);
//
//            if (savedRecommendations.isEmpty()) {
//                return ResponseEntity.noContent().build();
//            }
//
//            return ResponseEntity.ok(savedRecommendations);
//        } catch (IllegalArgumentException e) {
//            System.err.println("AI 추천 생성 중 오류: " + e.getMessage());
//            return ResponseEntity.notFound().build();
//        } catch (Exception e) {
//            System.err.println("AI 추천 생성 중 예기치 않은 오류: " + e.getMessage());
//            e.printStackTrace();
//            return ResponseEntity.internalServerError().build();
//        }
//    }

}
