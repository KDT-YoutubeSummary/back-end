package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.Util.TextCleaner;
import com.kdt.yts.YouSumback.model.entity.AudioTranscript;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.AudioTranscriptRepository;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final VideoRepository videoRepository;
    private final AudioTranscriptRepository transcriptRepository;
    private final TextCleaner textCleaner;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String s3BucketName;

    @Value("${whisper.server.url}")
    private String whisperServerUrl;  // 예: http://whisper:8000

    public Long extractYoutubeIdAndRunWhisper(String originalUrl, String purpose) throws Exception {
        String youtubeId = extractYoutubeId(originalUrl);
        if (youtubeId == null || youtubeId.isEmpty()) {
            throw new IllegalArgumentException("유효한 YouTube URL이 아닙니다.");
        }

        // 1. Video 엔티티 등록 (중복 방지)
        Video video = videoRepository.findByYoutubeId(youtubeId).orElseGet(() -> {
            Video newVideo = new Video();
            newVideo.setYoutubeId(youtubeId);
            newVideo.setOriginalUrl(originalUrl);
            newVideo.setTitle("제목 없음");
            newVideo.setUploaderName("unknown");
            newVideo.setPublishedAt(LocalDateTime.now());
            return videoRepository.save(newVideo);
        });

        // 2. Whisper 서버 호출
        callWhisperServer(originalUrl);

        // 3. Whisper가 S3에 결과 올려놨다고 가정 -> S3에서 가져오기
        String s3Key = "whisper-results/" + youtubeId + ".txt";  // whisper-server가 이 패턴으로 저장하도록 합의
        String rawText = getObjectContentFromS3(s3Key);

        // 4. 텍스트 정제
        String cleanedText = textCleaner.clean(rawText);

        // 5. DB 저장
        AudioTranscript transcript = transcriptRepository.findByVideoId(video.getId())
                .map(existing -> {
                    existing.setTranscriptPath(s3Key);
                    existing.setCreatedAt(LocalDateTime.now());
                    return existing;
                }).orElseGet(() -> AudioTranscript.builder()
                        .video(video)
                        .youtubeId(youtubeId)
                        .transcriptPath(s3Key)
                        .createdAt(LocalDateTime.now())
                        .build());
        transcriptRepository.save(transcript);

        return video.getId();
    }

    // Whisper 서버 호출 (HTTP POST)
    private void callWhisperServer(String youtubeUrl) {
        RestTemplate restTemplate = new RestTemplate();
        String url = whisperServerUrl + "/run_whisper/";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("youtube_url", youtubeUrl);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Whisper 호출 실패: " + response.getBody());
            }
        } catch (Exception e) {
            throw new RuntimeException("Whisper 서버 호출 중 예외 발생", e);
        }
    }

    // 유튜브 ID 추출 (기존 유지)
    private String extractYoutubeId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        try {
            if (url.contains("v=")) {
                String[] parts = url.split("v=");
                String afterV = parts[1];
                return afterV.contains("&") ? afterV.split("&")[0] : afterV;
            } else if (url.contains("youtu.be/")) {
                String[] parts = url.split("youtu.be/");
                String afterSlash = parts[1];
                return afterSlash.contains("?") ? afterSlash.split("\\?")[0] : afterSlash;
            } else if (url.contains("youtube.com/embed/")) {
                String[] parts = url.split("embed/");
                String afterEmbed = parts[1];
                return afterEmbed.contains("?") ? afterEmbed.split("\\?")[0] : afterEmbed;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // S3에서 텍스트 읽기
    private String getObjectContentFromS3(String key) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3BucketName)
                .key(key)
                .build();

        ResponseInputStream<GetObjectResponse> s3is = s3Client.getObject(getObjectRequest);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(s3is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    // 정제된 텍스트 읽기 (기존 유지)
    public String readTranscriptText(Long videoId) throws IOException {
        AudioTranscript transcript = transcriptRepository.findByVideoId(videoId)
                .orElseThrow(() -> new NoSuchElementException("Transcript not found"));
        return getObjectContentFromS3(transcript.getTranscriptPath());
    }
}
