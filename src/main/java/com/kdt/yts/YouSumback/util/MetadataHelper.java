package com.kdt.yts.YouSumback.util;

import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataHelper {

    private final VideoRepository videoRepository;

    public String extractYoutubeId(String url) {
        if (url.contains("v=")) {
            return url.substring(url.indexOf("v=") + 2).split("&")[0];
        } else if (url.contains("youtube/")) {
            return url.substring(url.indexOf("youtube/") + 9).split("\\?")[0];
        } else if (url.contains("youtu.be/")) {
            return url.split("youtu.be/")[1].split("\\?")[0];
        }
        throw new IllegalArgumentException("유효하지 않은 유튜브 링크입니다.");
    }

    /**
     * 유튜브 ID 기반 Video 조회, 없으면 저장
     */
    // MetadataHelper.java

    public Video fetchAndSaveMetadata(String url) {
        String youtubeId = extractYoutubeId(url);

        return videoRepository.findByYoutubeId(youtubeId)
                .orElseGet(() -> {
                    log.warn("📦 영상 정보 없음. 임시 메타데이터로 저장: {}", youtubeId);
                    Video video = Video.builder()
                            .youtubeId(youtubeId)
                            .originalUrl(url)
                            .title("제목 없음")
                            .uploaderName("알 수 없음")
                            .thumbnailUrl("https://via.placeholder.com/640x360.png?text=No+Thumbnail")
                            .viewCount(0L)
                            .publishedAt(LocalDateTime.now())
                            .durationSeconds(0)
                            .originalLanguageCode("unknown")
                            .build();

                    return videoRepository.save(video);
                });
    }

}
