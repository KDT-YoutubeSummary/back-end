package com.kdt.yts.YouSumback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.yts.YouSumback.model.entity.AudioTranscript;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.AudioTranscriptRepository;
import com.kdt.yts.YouSumback.util.MetadataHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final AudioTranscriptRepository audioTranscriptRepository;
    private final MetadataHelper metadataHelper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public void extractYoutubeIdAndRunWhisper(String url, String userPrompt) {
        try {
            // 1. 유튜브 ID 추출
            String youtubeId = metadataHelper.extractYoutubeId(url);
            log.info("🎯 유튜브 ID 추출: {}", youtubeId);

            // 2. Video 조회 또는 생성
            Video video = metadataHelper.fetchOrCreateMetadata(url);
            log.info("📦 Video 조회 완료: id={}, youtubeId={}", video.getId(), video.getYoutubeId());

            // 3. 기존 Transcript 확인
            AudioTranscript transcript = audioTranscriptRepository.findByVideoId(video.getId()).orElse(null);
            if (transcript != null) {
                log.info("📘 기존 Transcript 존재: ID: {}, s3_path: {}", transcript.getId(), transcript.getTranscriptPath());
                return; // 중복 저장 방지
            }

            // 4. Whisper 서버 요청
            String whisperServerUrl = "http://whisper-server:8000/transcribe";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("videoUrl", url);
            body.put("youtubeId", youtubeId);
            if (userPrompt != null && !userPrompt.isBlank()) {
                body.put("userPrompt", userPrompt);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            log.info("🚀 Whisper 서버에 요청 시작: {}", whisperServerUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(whisperServerUrl, request, String.class);
            log.info("📥 Whisper 응답 수신: Status={}, Body={}", response.getStatusCode(), response.getBody());

            // 5. 응답 파싱 및 저장
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                String s3Path = json.path("s3_path").asText();

                List<String> allowedExtensions = List.of(".txt", ".vtt", ".srt");
                boolean valid = allowedExtensions.stream().anyMatch(s3Path::endsWith);

                if (s3Path == null || s3Path.isBlank() || !valid) {
                    throw new IllegalStateException("❌ Whisper 응답 확장자 형식이 잘못됨: " + s3Path);
                }

                AudioTranscript newTranscript = AudioTranscript.builder()
                        .video(video)
                        .youtubeId(youtubeId)
                        .transcriptPath(s3Path)
                        .createdAt(LocalDateTime.now())
                        .build();

                audioTranscriptRepository.save(newTranscript);
                log.info("✅ AudioTranscript 저장 완료: {}", newTranscript.getId());
            } else {
                throw new IllegalStateException("❌ Whisper 응답 실패 또는 본문 없음: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("🔥 Whisper 처리 중 예외 발생", e);
            throw new RuntimeException("Whisper 처리 실패", e);
        }
    }
}
