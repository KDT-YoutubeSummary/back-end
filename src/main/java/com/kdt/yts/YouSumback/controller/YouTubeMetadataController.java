package com.kdt.yts.YouSumback.controller;

import com.google.api.services.youtube.model.Video;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import com.kdt.yts.YouSumback.service.YouTubeMetadataService;
import com.kdt.yts.YouSumback.service.client.YouTubeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// YouTube 영상 메타데이터를 저장하는 컨트롤러
// 수동 저장 API
// 실제 흐름에서는 요약 요청이 들어올 때, 그 영상의 메타데이터가 자동으로 저장되도록 처리
@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
// YouTube 영상 메타데이터를 저장하는 컨트롤러 -> 수동 저장 API
// 실제 흐름에서는 요약 요청이 들어올 때, 그 영상의 메타데이터가 자동으로 저장되도록 처리
public class YouTubeMetadataController {

    private final YouTubeMetadataService youTubeMetadataService;
    private final VideoRepository videoRepository;


    // POST /api/youtube/save?url=http://youtube.com/...
    @PostMapping("/save")
    public ResponseEntity<?> saveVideo(@RequestParam("url") String youtubeVideoId) {
        try {
            youTubeMetadataService.saveVideoMetadata(youtubeVideoId);
            return ResponseEntity.ok("영상 메타데이터 저장 완료: " + youtubeVideoId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("저장 실패: " + e.getMessage());
        }
    }

    // GET /api/youtube/title?url=https://youtube.com/...
    // YouTube 영상 ID로 제목을 가져오는 API
    @GetMapping("/title")
    public ResponseEntity<String> getVideoTitle(@RequestParam("url") String url) {
        try {
            String youtubeId = youTubeMetadataService.extractYoutubeId(url);
            return videoRepository.findByYoutubeId(youtubeId)
                    .map(video -> ResponseEntity.ok(video.getTitle()))
                    .orElseGet(() -> ResponseEntity.status(404).body("해당 영상이 존재하지 않음"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("잘못된 유튜브 URL입니다.");
        }
    }
}
