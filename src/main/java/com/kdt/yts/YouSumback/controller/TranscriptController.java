package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.model.dto.response.TranscriptLookupResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public class TranscriptController {
    @GetMapping
    public ResponseEntity<?> getTranscript(@RequestParam("video_id") Long videoId) {
        try {
            TranscriptLookupResponseDto transcript = transcriptService.getTranscriptByVideoId(videoId);
            return ResponseEntity.ok(new ApiResponse(200, "STT 조회 완료", transcript));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(new ApiResponse(404, e.getMessage(), null));
        }
    }

}
