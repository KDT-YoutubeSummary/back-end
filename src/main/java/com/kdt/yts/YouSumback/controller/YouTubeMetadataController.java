package com.kdt.yts.YouSumback.controller;

import com.kdt.yts.YouSumback.repository.VideoRepository;
import com.kdt.yts.YouSumback.service.YouTubeMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YouTubeMetadataController {

    private final YouTubeMetadataService youTubeMetadataService;
    private final VideoRepository videoRepository;

    // ✅ 유튜브 영상 메타데이터 저장 (url에서 youtubeId 추출)
    @PostMapping("/save")
    public ResponseEntity<?> saveVideo(@RequestParam("url") String url) {
        try {
            youTubeMetadataService.saveVideoMetadataFromUrl(url);
            return ResponseEntity.ok("영상 메타데이터 저장 완료: " + url);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("저장 실패: " + e.getMessage());
        }
    }

    // ✅ 유튜브 제목 조회
    @GetMapping("/title")
    public ResponseEntity<?> getVideoTitle(@RequestParam("url") String url) {
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
