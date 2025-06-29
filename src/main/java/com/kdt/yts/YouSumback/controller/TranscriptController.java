package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.TranscriptSaveRequestDTO;
import com.kdt.yts.YouSumback.service.TranscriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/scripts")
@RequiredArgsConstructor
@Tag(name = "스크립트", description = "스크립트 조회, 저장, 삭제 API")
public class TranscriptController {

    private final TranscriptService transcriptService;

    @Operation(summary = "대본 생성", description = "음성/영상 파일에서 대본을 추출합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대본 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식")
    })
    @PostMapping("/stt")
    public ResponseEntity<?> saveTranscript(@RequestBody TranscriptSaveRequestDTO requestDTO) {
        try {
            transcriptService.extractYoutubeIdAndRunWhisper(requestDTO.getOriginalUrl(), requestDTO.getUserPrompt());
            return ResponseEntity.status(201).body("✅ Whisper 실행 완료");
        } catch (Exception e) {
            log.error("❌ Whisper 실행 중 예외 발생", e);
            return ResponseEntity.status(500).body(Map.of(
                    "message", "요약 생성 중 서버 오류가 발생했습니다.",
                    "error", e.getMessage()
            ));
        }
    }

}

