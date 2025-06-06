package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.model.entity.AudioTranscript;
import com.kdt.yts.YouSumback.model.entity.SummaryType;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.AudioTranscriptRepository;
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
    private final TranscriptService transcriptService;
    private final SummaryService summaryService;
    private final AudioTranscriptRepository audioTranscriptRepository;

    // ✅ 유튜브 URL에서 ID 추출
    public String extractYoutubeId(String url) {
        if (url.contains("v=")) {
            return url.substring(url.indexOf("v=") + 2).split("&")[0];
        } else if (url.contains("youtu.be/")) {
            return url.substring(url.indexOf("youtu.be/") + 9).split("\\?")[0];
        }
        throw new IllegalArgumentException("유효하지 않은 유튜브 링크입니다.");
    }

    // ✅ 영상 메타데이터 저장 (중복 방지)
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

    // ✅ URL로 메타데이터 저장
    public void saveVideoMetadataFromUrl(String url) throws Exception {
        String youtubeId = extractYoutubeId(url);
        saveVideoMetadata(youtubeId);
    }

    // ✅ 전체 처리 (요약 포함) 후 DTO 반환
    public SummaryResponseDTO processVideoFromUrl(String url, String userPrompt, String summaryType, Long userId) throws Exception {
        try {
            saveVideoMetadataFromUrl(url);
        } catch (IllegalArgumentException e) {
            if (!e.getMessage().contains("이미 저장된 영상")) {
                throw e;
            }
        }

        // Whisper로 텍스트 추출
        transcriptService.extractYoutubeIdAndRunWhisper(url, userPrompt);

        // 자막 가져오기
        String youtubeId = extractYoutubeId(url);
        AudioTranscript transcript = audioTranscriptRepository.findByYoutubeId(youtubeId)
                .orElseThrow(() -> new RuntimeException("Transcript not found"));

        // 요약 요청 DTO 구성
        SummaryRequestDTO dto = new SummaryRequestDTO();
        dto.setTranscriptId(transcript.getId());
        dto.setUserId(userId);
        dto.setText(transcript.getTranscriptText());
        dto.setUserPrompt(userPrompt);

        // ✅ summaryType이 null이면 기본값 지정
        if (summaryType == null || summaryType.isBlank()) {
            summaryType = "THREE_LINE";
        }

        try {
            dto.setSummaryType(SummaryType.valueOf(summaryType.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("❌ 지원하지 않는 요약 타입입니다: " + summaryType);
        }

        // 요약 수행 (SummaryResponseDTO 반환)
        return summaryService.summarize(dto);
    }
}
