package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.repository.SummaryRepository;
import com.kdt.yts.YouSumback.security.CustomUserDetails;
import com.kdt.yts.YouSumback.service.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/summaries")
@RequiredArgsConstructor
@Tag(name = "요약", description = "텍스트 요약 관련 API")
public class SummaryController {


    private final SummaryService summaryService;
    private final SummaryRepository summaryRepository;

    // 요약 생성
    @Operation(summary = "텍스트 요약 생성", description = "입력된 텍스트를 분석하여 요약을 생성합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "요약 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 입력 데이터")
    })
    @GetMapping("/{transcriptId}")
    public ResponseEntity<?> getSummaryByTranscript(@PathVariable Long transcriptId,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();

        return summaryRepository.findByUserIdAndAudioTranscriptId(transcriptId, userId)
                .map(summary -> {
                    var transcript = summary.getAudioTranscript();
                    var video = transcript.getVideo();

                    // ✅ UserLibrary 조회
                    var libraryOpt = summaryService.findUserLibraryByUserAndSummary(userId, summary);

                    List<String> tagNames = libraryOpt
                            .map(lib -> lib.getUserLibraryTag() != null
                                    ? lib.getUserLibraryTag().stream()
                                    .map(ult -> ult.getTag().getTagName())
                                    .toList()
                                    : List.<String>of())
                            .orElse(List.of());

                    // ✅ DTO 구성
                    SummaryResponseDTO dto = SummaryResponseDTO.builder()
                            .summaryId(summary.getId())
                            .transcriptId(transcript.getId())
                            .videoId(video.getId())
                            .summary(summary.getSummaryText())
                            .tags(tagNames)
                            .title(video.getTitle())
                            .thumbnailUrl(video.getThumbnailUrl())
                            .uploaderName(video.getUploaderName())
                            .viewCount(video.getViewCount())
                            .languageCode(summary.getLanguageCode())
                            .createdAt(summary.getCreatedAt())
                            .build();

                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }


        // youtube/upload 에서 요청
//    // 요약 요청을 처리하는 API 엔드포인트
//    @PostMapping
//    public SummaryResponseDTO summarize(@Valid @RequestBody SummaryRequestDTO request) {
//        return summaryService.summarize(request);
//    }
}
