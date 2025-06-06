package com.kdt.yts.YouSumback.service;

import com.google.api.services.youtube.model.Video; // Google API Video
import com.kdt.yts.YouSumback.repository.VideoRepository;
import com.kdt.yts.YouSumback.service.client.YouTubeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
// YoutubeMetadataService에서 처리됨
public class VideoService {

    private final YouTubeClient youTubeClient;
    private final VideoRepository videoRepository;

    // ✅ 1. 유튜브 URL에서 youtubeId 추출
    public String extractYoutubeId(String url) {
        if (url.contains("v=")) {
            return url.substring(url.indexOf("v=") + 2).split("&")[0];
        } else if (url.contains("youtu.be/")) {
            return url.substring(url.indexOf("youtu.be/") + 9).split("\\?")[0];
        }
        throw new IllegalArgumentException("유효하지 않은 유튜브 링크입니다.");
    }

    // ✅ 2. YouTube 메타데이터 저장
    public com.kdt.yts.YouSumback.model.entity.Video saveVideoMetadata(String youtubeId) throws Exception {

        // 이미 존재하면 그대로 반환
        Optional<com.kdt.yts.YouSumback.model.entity.Video> existing =
                videoRepository.findByYoutubeId(youtubeId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 유튜브 API 호출
        Video youtubeVideo = youTubeClient.fetchVideoById(youtubeId);
        if (youtubeVideo == null) {
            throw new IllegalStateException("유튜브 API 응답 없음");
        }

        if (youtubeVideo.getSnippet() == null || youtubeVideo.getStatistics() == null) {
            throw new IllegalStateException("영상 정보가 불완전합니다.");
        }

        // 🔽 명시적 경로로 엔티티 builder 사용
        com.kdt.yts.YouSumback.model.entity.Video video =
                com.kdt.yts.YouSumback.model.entity.Video.builder()
                        .id(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE)
                        .youtubeId(youtubeId)
                        .title(youtubeVideo.getSnippet().getTitle())
                        .originalUrl("https://www.youtube.com/watch?v=" + youtubeId)
                        .uploaderName(youtubeVideo.getSnippet().getChannelTitle())
                        .thumbnailUrl(youtubeVideo.getSnippet().getThumbnails().getDefault().getUrl())
                        .viewCount(youtubeVideo.getStatistics().getViewCount().longValue())
                        .publishedAt(LocalDateTime.parse(
                                youtubeVideo.getSnippet().getPublishedAt().toStringRfc3339(),
                                DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        ))
                        .build();

        return videoRepository.save(video);
    }
}
