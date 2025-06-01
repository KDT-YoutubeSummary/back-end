package com.YouSumback.controller;

import com.YouSumback.model.dto.request.VideoRegisterRequestDto;
import com.YouSumback.model.dto.response.VideoResponseDto;
import com.YouSumback.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/src")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @PostMapping
    public ResponseEntity<VideoResponseDto> registerVideo(@RequestBody VideoRegisterRequestDto requestDto) {
        VideoResponseDto responseDto = videoService.registerVideo(requestDto);
        return ResponseEntity.status(201).body(responseDto);
    }
}

