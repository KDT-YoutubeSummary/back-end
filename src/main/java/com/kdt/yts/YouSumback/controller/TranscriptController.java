package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.TranscriptSaveRequestDTO;
import com.kdt.yts.YouSumback.service.TranscriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scripts")
@RequiredArgsConstructor
@Tag(name = "대본", description = "음성 및 영상 대본 관리 API")
// Whisper만 별도 테스트하는 컨트롤러
public class TranscriptController {

    private final TranscriptService transcriptService;

    @Operation(summary = "대본 생성", description = "음성/영상 파일에서 대본을 추출합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대본 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식")
    })
    @PostMapping("/stt")
    public ResponseEntity<String> saveTranscript(@RequestBody TranscriptSaveRequestDTO requestDTO) {
        try {
            transcriptService.extractYoutubeIdAndRunWhisper(requestDTO.getOriginalUrl(), requestDTO.getUserPrompt());
            return ResponseEntity.ok("✅ Whisper 실행 완료");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Whisper 실행 실패: " + e.getMessage());
        }
    }
}
