package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.model.entity.SummaryType;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import com.kdt.yts.YouSumback.service.client.YouTubeClient;
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
    private final TranscriptService transcriptService;
    private final SummaryService summaryService;

    // ✅ 유튜브 URL에서 ID 추출 (변경 없음)
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

    // ✅ 영상 메타데이터 저장 (변경 없음)
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

    // ✅ URL로 메타데이터 저장 (변경 없음)
    public void saveVideoMetadataFromUrl(String url) throws Exception {
        String youtubeId = extractYoutubeId(url);
        saveVideoMetadata(youtubeId);
    }

    // ⭐️⭐️⭐️ [핵심 수정] 전체 처리 흐름을 단순화하고 역할을 명확하게 분리 ⭐️⭐️⭐️
    public SummaryResponseDTO processVideoFromUrl(String url, String userPrompt, SummaryType summaryType, Long userId) throws Exception {
        // 1. 영상 메타데이터 저장 (없으면 생성)
        saveVideoMetadataFromUrl(url);

        // 2. Whisper 실행 및 S3 경로 DB에 저장
        // TranscriptService가 whisper-server와 통신하고, 결과물인 S3 파일 경로를 DB에 저장합니다.
        transcriptService.extractYoutubeIdAndRunWhisper(url, userPrompt);

        // 3. 요약 요청 DTO 구성
        // SummaryServiceImpl은 이제 URL만으로 S3 경로를 포함한 모든 정보를 찾을 수 있습니다.
        SummaryRequestDTO dto = new SummaryRequestDTO();
        dto.setOriginalUrl(url);
        dto.setSummaryType(summaryType != null ? summaryType : BASIC);
        dto.setUserPrompt(userPrompt);

        // 4. 요약 서비스 호출
        // SummaryServiceImpl 내부에서 S3 파일을 읽고 요약을 수행합니다.
        return summaryService.summarize(dto, userId);
    }
}
