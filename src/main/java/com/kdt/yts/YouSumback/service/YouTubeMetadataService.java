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

    // YouTube 영상 메타데이터를 저장하는 메서드
    public void saveVideoMetadata(String youtubeVideoId) throws Exception {
        // 구글 API에서 받아온 영상 데이터
        com.google.api.services.youtube.model.Video youtubeVideo = youTubeClient.fetchVideoById(youtubeVideoId);
        if (youtubeVideo == null) return;

        // 중복 확인
        if (videoRepository.findByYoutubeId(youtubeVideoId).isPresent()) {
            throw new IllegalArgumentException("이미 저장된 영상입니다: " + youtubeVideoId);
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
