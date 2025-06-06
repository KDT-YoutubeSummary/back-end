package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.TranscriptSaveRequestDTO;
import com.kdt.yts.YouSumback.service.TranscriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/src")
@RequiredArgsConstructor
// Whisper만 별도 테스트하는 컨트롤러
public class TranscriptController {

    private final TranscriptService transcriptService;

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
