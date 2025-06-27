// TranscriptService.java
package com.kdt.yts.YouSumback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.yts.YouSumback.model.entity.AudioTranscript;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.AudioTranscriptRepository;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import com.kdt.yts.YouSumback.util.MetadataHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final RestTemplate restTemplate;
    private final VideoRepository videoRepository;
    private final AudioTranscriptRepository audioTranscriptRepository;
    private final MetadataHelper metadataHelper;
    private final ObjectMapper objectMapper;

    @Transactional
    public void extractYoutubeIdAndRunWhisper(String url, String userPrompt) throws Exception {
        String youtubeId = metadataHelper.extractYoutubeId(url);

        Video video = videoRepository.findByYoutubeId(youtubeId)
                .orElseThrow(() -> new RuntimeException("Video not found for YouTube ID: " + youtubeId));

        Optional<AudioTranscript> existingTranscript = audioTranscriptRepository.findByVideoId(video.getId());
        if (existingTranscript.isPresent() && existingTranscript.get().getTranscriptPath() != null) {
            System.out.println("이미 처리된 Transcript가 존재합니다. ID: " + existingTranscript.get().getId());
            return;
        }

        String whisperServerUrl = "http://whisper-server:8000/transcribe";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("videoUrl", url, "youtubeId", youtubeId);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(whisperServerUrl, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                String s3Path = responseBody.path("s3_path").asText();
                if (s3Path == null || s3Path.isEmpty()) {
                    throw new RuntimeException("Whisper 서버가 S3 경로를 반환하지 않았습니다.");
                }

                System.out.println("✅ Whisper 서버로부터 받은 S3 경로: " + s3Path);

                AudioTranscript transcript = existingTranscript.orElse(new AudioTranscript());
                transcript.setVideo(video);
                transcript.setYoutubeId(youtubeId);
                transcript.setTranscriptPath(s3Path);
                transcript.setCreatedAt(LocalDateTime.now());
                audioTranscriptRepository.save(transcript);

            } else {
                throw new RuntimeException("Whisper 서버 처리 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ Whisper 서버 통신 중 오류 발생: " + e.getMessage());
            throw new RuntimeException("Whisper 서버 통신 중 오류가 발생했습니다.", e);
        }
    }
}