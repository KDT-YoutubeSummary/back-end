package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.TranscriptSaveRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.model.entity.SummaryType;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import com.kdt.yts.YouSumback.security.CustomUserDetails;
import com.kdt.yts.YouSumback.service.TranscriptService;
import com.kdt.yts.YouSumback.service.YouTubeMetadataService;
import com.kdt.yts.YouSumback.util.MetadataHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Tag(name = "YouTube 메타데이터", description = "YouTube 동영상 메타데이터 관련 API")
@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YouTubeMetadataController {

    private final YouTubeMetadataService youTubeMetadataService;
    private final TranscriptService transcriptService;
    private final VideoRepository videoRepository;
    private final MetadataHelper metadataHelper;

    @Operation(summary = "영상 메타데이터 저장", description = "유튜브 영상의 메타데이터를 저장합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메타데이터 저장 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PostMapping("/save")
    public ResponseEntity<?> saveVideo(@RequestParam("url") String url) {
        try {
            youTubeMetadataService.saveVideoMetadataFromUrl(url);
            return ResponseEntity.ok("영상 메타데이터 저장 완료: " + url);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("저장 실패: " + e.getMessage());
        }
    }

    @Operation(summary = "영상 메타데이터 조회", description = "유튜브 영상 ID로 메타데이터를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메타데이터 조회 성공"),
            @ApiResponse(responseCode = "404", description = "영상을 찾을 수 없음")
    })
    @GetMapping("/title")
    public ResponseEntity<?> getVideoTitle(@RequestParam("url") String url) {
        try {
            String youtubeId = metadataHelper.extractYoutubeId(url);
            return videoRepository.findByYoutubeId(youtubeId)
                    .map(video -> ResponseEntity.ok(video.getTitle()))
                    .orElseGet(() -> ResponseEntity.status(404).body("해당 영상이 존재하지 않음"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("잘못된 유튜브 URL입니다.");

        }

    }

    @Operation(summary = "유튜브 영상 업로드", description = "유튜브 링크를 업로드해 요약을 요청합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "요약 성공"),
            @ApiResponse(responseCode = "404", description = "요약 실패")
    })
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFromUrl(@RequestBody TranscriptSaveRequestDTO request,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long userId = userDetails.getUserId();

            // DTO의 summaryType이 Enum 타입으로 변경되었으므로, 변환 로직을 단순화합니다.
            // 만약 summaryType이 null이면 기본값으로 BASIC을 사용합니다.
            SummaryType summaryType = request.getSummaryType() != null ? request.getSummaryType() : SummaryType.BASIC;

            transcriptService.extractYoutubeIdAndRunWhisper(request.getVideoUrl(), request.getUserPrompt());

            SummaryResponseDTO responseDTO = youTubeMetadataService.summarizeWithMetadata(
                    request.getVideoUrl(),
                    request.getUserPrompt(),
                    summaryType,
                    userId
            );

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "요약 생성 완료");
            response.put("data", responseDTO);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("해당 YouTube ID에 해당하는 영상 없음")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("code", 404);
                errorResponse.put("message", "먼저 영상 메타데이터를 저장해야 합니다.");
                errorResponse.put("error", e.getMessage());
                errorResponse.put("data", null);
                return ResponseEntity.status(404).body(errorResponse);
            } else {
                throw e;
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "요약 생성 중 서버 오류가 발생했습니다.");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.status(500).body(errorResponse);
        }

    }
}