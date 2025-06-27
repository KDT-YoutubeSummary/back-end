// YouTubeMetadataService.java
package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.model.entity.SummaryType;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import com.kdt.yts.YouSumback.service.client.YouTubeClient;
import com.kdt.yts.YouSumback.Util.MetadataHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.kdt.yts.YouSumback.model.entity.SummaryType.BASIC;

@Service
@RequiredArgsConstructor
public class YouTubeMetadataService {

    private final YouTubeClient youTubeClient;
    private final VideoRepository videoRepository;
    private final SummaryService summaryService;
    private final MetadataHelper metadataHelper;

    public void saveVideoMetadata(String youtubeVideoId) throws Exception {
        if (videoRepository.findByYoutubeId(youtubeVideoId).isPresent()) {
            System.out.println("📌 이미 저장된 영상입니다: " + youtubeVideoId);
            return;
        }

        var youtubeVideo = youTubeClient.fetchVideoById(youtubeVideoId);
        if (youtubeVideo == null || youtubeVideo.getSnippet() == null || youtubeVideo.getStatistics() == null) {
            throw new IllegalStateException("영상 정보가 불완전합니다.");
        }

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

    public void saveVideoMetadataFromUrl(String url) throws Exception {
        String youtubeId = metadataHelper.extractYoutubeId(url);
        saveVideoMetadata(youtubeId);
    }

    public SummaryResponseDTO summarizeWithMetadata(String url, String userPrompt, SummaryType summaryType, Long userId) throws Exception {
        saveVideoMetadataFromUrl(url);

        SummaryRequestDTO dto = new SummaryRequestDTO();
        dto.setOriginalUrl(url);
        dto.setSummaryType(summaryType != null ? summaryType : BASIC);
        dto.setUserPrompt(userPrompt);

        return summaryService.summarize(dto, userId);
    }
}
