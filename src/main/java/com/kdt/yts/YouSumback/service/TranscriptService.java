package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.Util.TextCleaner;
import com.kdt.yts.YouSumback.model.entity.AudioTranscript;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.AudioTranscriptRepository;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final VideoRepository videoRepository;
    private final AudioTranscriptRepository transcriptRepository;
    private final TextCleaner textCleaner;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String s3BucketName;

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

        // 2. Whisper 서버 REST 호출
        RestTemplate restTemplate = new RestTemplate();
        String whisperServerUrl = "http://whisper-server:8000/transcribe";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("youtubeId", youtubeId);
        requestBody.put("videoUrl", originalUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(whisperServerUrl, requestEntity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Whisper 서버 호출 실패: " + response.getStatusCode());
        }

        String rawText = response.getBody();

        // 3. 텍스트 정제
        String cleanedText = textCleaner.clean(rawText);

        // 4. S3 업로드
        String s3Key = "whisper-results/" + youtubeId + ".txt";
        uploadTextToS3(s3Key, cleanedText);

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

    // 유튜브 ID 추출 유틸
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

    // S3 업로드 함수
    private void uploadTextToS3(String key, String text) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3BucketName)
                .key(key)
                .contentType("text/plain")
                .build();

        s3Client.putObject(putRequest, RequestBody.fromString(text, StandardCharsets.UTF_8));
    }
}
