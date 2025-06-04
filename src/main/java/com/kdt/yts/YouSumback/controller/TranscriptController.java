package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.request.TranscriptSaveRequestDto;
import com.kdt.yts.YouSumback.model.dto.request.TranscriptDeleteRequestDto;
import com.kdt.yts.YouSumback.model.dto.response.TranscriptSaveResponseDto;
import com.kdt.yts.YouSumback.model.dto.response.TranscriptLookupResponseDto;
import com.kdt.yts.YouSumback.service.TranscriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/src/stt")
@RequiredArgsConstructor
public class TranscriptController {

    private final TranscriptService transcriptService;

    // ✅ url 기반 STT 저장 요청
    @PostMapping
    public ResponseEntity<TranscriptSaveResponseDto> saveTranscript(@RequestBody TranscriptSaveRequestDto requestDto) throws Exception {
        TranscriptSaveResponseDto response = transcriptService.saveTranscript(requestDto);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TranscriptLookupResponseDto>> getTranscriptList(@RequestParam("youtube_id") String youtubeId) {
        List<TranscriptLookupResponseDto> responseList = transcriptService.getTranscriptListByYoutubeId(youtubeId);
        return ResponseEntity.ok(responseList);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteTranscript(@RequestBody TranscriptDeleteRequestDto requestDto) {
        transcriptService.deleteTranscript(requestDto.getTranscriptId());
        return ResponseEntity.noContent().build();
    }
}
