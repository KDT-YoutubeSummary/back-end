package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import com.kdt.yts.YouSumback.service.client.YouTubeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class YouTubeMetadataService {

    private final YouTubeClient youTubeClient; // YouTube API 클라이언트
    private final VideoRepository videoRepository; // JPA 레포지토리

    // 유튜브 링크에서 ID를 추출하는 메서드
    public String extractYoutubeId(String url) {
        if (url.contains("v=")) {
            return url.substring(url.indexOf("v=") + 2);
        } else if (url.contains("youtu.be/")) {
            return url.substring(url.indexOf("youtu.be/") + 9);
        }
        throw new IllegalArgumentException("유효하지 않은 유튜브 링크입니다.");
    }

    // YouTube 영상 메타데이터를 저장하는 메서드
    public void saveVideoMetadata(String youtubeVideoId) throws Exception {
        // 구글 API에서 받아온 영상 데이터
        com.google.api.services.youtube.model.Video youtubeVideo = youTubeClient.fetchVideoById(youtubeVideoId);
        if (youtubeVideo == null) return;

        // 중복 확인
        if (videoRepository.findByYoutubeId(youtubeVideoId).isPresent()) {
            throw new IllegalArgumentException("이미 저장된 영상입니다: " + youtubeVideoId);
        }

        // 영상 정보가 불완전한 경우 예외 처리
        if (youtubeVideo.getSnippet() == null || youtubeVideo.getStatistics() == null) {
            throw new IllegalStateException("영상 정보가 불완전합니다.");
        }

        // JPA 엔티티로 변환
        Video videoEntity = Video.builder()
                .videoId(UUID.randomUUID().toString())
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
}
