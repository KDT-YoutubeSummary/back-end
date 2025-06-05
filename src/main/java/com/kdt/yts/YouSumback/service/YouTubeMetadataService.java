package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import com.kdt.yts.YouSumback.service.client.YouTubeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class YouTubeMetadataService {

    private final YouTubeClient youTubeClient;
    private final VideoRepository videoRepository;

    // ✅ 유튜브 URL에서 ID 추출
    public String extractYoutubeId(String url) {
        if (url.contains("v=")) {
            return url.substring(url.indexOf("v=") + 2).split("&")[0]; // v=abc&list=... 방지
        } else if (url.contains("youtu.be/")) {
            return url.substring(url.indexOf("youtu.be/") + 9).split("\\?")[0]; // 파라미터 제거
        }
        throw new IllegalArgumentException("유효하지 않은 유튜브 링크입니다.");
    }

    // ✅ YouTube 영상 메타데이터 저장 (youtubeId 기준)
    public void saveVideoMetadata(String youtubeVideoId) throws Exception {
        // 이미 저장된 영상인지 확인
        if (videoRepository.findByYoutubeId(youtubeVideoId).isPresent()) {
            throw new IllegalArgumentException("이미 저장된 영상입니다: " + youtubeVideoId);
        }

        // 유튜브 API로 메타데이터 조회
        com.google.api.services.youtube.model.Video youtubeVideo = youTubeClient.fetchVideoById(youtubeVideoId);
        if (youtubeVideo == null || youtubeVideo.getSnippet() == null || youtubeVideo.getStatistics() == null) {
            throw new IllegalStateException("영상 정보가 불완전합니다.");
        }

        // 엔티티로 변환 후 저장 (videoId는 자동 생성)
        Video videoEntity = Video.builder()
                .youtubeId(youtubeVideoId)
                .title(youtubeVideo.getSnippet().getTitle())
                .originalUrl("https://www.youtube.com/watch?v=" + youtubeVideoId)
                .uploaderName(youtubeVideo.getSnippet().getChannelTitle())
                .thumbnailUrl(youtubeVideo.getSnippet().getThumbnails().getDefault().getUrl())
                .viewCount(youtubeVideo.getStatistics().getViewCount().longValue())
                .publishedAt(LocalDateTime.parse(
                        youtubeVideo.getSnippet().getPublishedAt().toStringRfc3339(),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME
                ))
                .build();

        videoRepository.save(videoEntity);
    }

    // ✅ URL 전체를 받아서 저장하는 보조 메서드 (추출 포함)
    public void saveVideoMetadataFromUrl(String url) throws Exception {
        String youtubeId = extractYoutubeId(url);
        saveVideoMetadata(youtubeId);
    }
}