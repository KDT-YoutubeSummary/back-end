package com.YouSumback.controller;

import com.YouSumback.model.dto.request.TranscriptSaveRequestDto;
import com.YouSumback.model.dto.response.TranscriptSaveResponseDto;
import com.YouSumback.service.TranscriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.YouSumback.model.dto.request.TranscriptDeleteRequestDto;


import com.YouSumback.model.dto.response.TranscriptLookupResponseDto;
import org.springframework.http.MediaType;

import java.util.List;




@RestController
@RequestMapping("/api/src/stt")
public class TranscriptController {

    @Autowired
    private TranscriptService transcriptService;

    @PostMapping
    public ResponseEntity<TranscriptSaveResponseDto> saveTranscript(@RequestBody TranscriptSaveRequestDto requestDto) {
        TranscriptSaveResponseDto response = transcriptService.saveTranscript(requestDto);
        return ResponseEntity.status(201).body(response);
    }
    @GetMapping
    public ResponseEntity<List<TranscriptLookupResponseDto>> getTranscript(@RequestParam("video_id") String videoId) {
        List<TranscriptLookupResponseDto> responseList = transcriptService.getTranscriptListByVideoId(videoId);
        return ResponseEntity.ok(responseList);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteTranscript(@RequestBody TranscriptDeleteRequestDto requestDto) {
        transcriptService.deleteTranscript(requestDto.getTranscriptId());
        return ResponseEntity.noContent().build(); // 204 응답
    }
    

}

