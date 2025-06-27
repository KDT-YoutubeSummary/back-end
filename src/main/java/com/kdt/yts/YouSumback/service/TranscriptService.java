package com.kdt.yts.YouSumback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.yts.YouSumback.model.entity.AudioTranscript;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.AudioTranscriptRepository;
import com.kdt.yts.YouSumback.util.MetadataHelper;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final RestTemplate restTemplate;
    private final VideoRepository videoRepository;
    private final AudioTranscriptRepository audioTranscriptRepository;
    private final MetadataHelper metadataHelper;
    private final ObjectMapper objectMapper;

    @Transactional
    public void extractYoutubeIdAndRunWhisper(String url, String userPrompt) {
        try {
            // 1. 유튜브 ID 추출
            String youtubeId = metadataHelper.extractYoutubeId(url);
            log.info("📺 유튜브 ID 추출: {}", youtubeId);

            // 2. Video 조회 또는 임시 저장
            Video video = metadataHelper.fetchOrCreateMetadata(url);
            log.info("🎞️ Video 조회 또는 저장 완료: ID={}, Title={}", video.getId(), video.getTitle());

            // 3. 기존 Transcript 확인
            AudioTranscript transcript = audioTranscriptRepository.findByVideoId(video.getId()).orElse(null);
            if (transcript != null) {
                log.info("🔁 기존 Transcript 존재. ID: {}, S3 경로: {}", transcript.getId(), transcript.getTranscriptPath());
            }

            // 4. Whisper 서버 요청 준비
            String whisperServerUrl = "http://whisper-server:8000/transcribe";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> request = new HttpEntity<>(Map.of(
                    "videoUrl", url,
                    "youtubeId", youtubeId,
                    "userPrompt", userPrompt != null ? userPrompt : ""
            ), headers);

            log.info("📤 Whisper 서버에 요청 시작: {}", whisperServerUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(whisperServerUrl, request, String.class);
            log.info("📥 Whisper 응답 수신: Status={}, Body={}", response.getStatusCode(), response.getBody());

            // 5. 응답 파싱 및 저장
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                String s3Path = json.path("s3_path").asText();

                if (s3Path == null || s3Path.isBlank() || !s3Path.endsWith(".txt")) {
                    throw new IllegalStateException("❌ Whisper 응답에 유효한 s3_path 없음: " + s3Path);
                }

                log.info("✅ Whisper 처리 성공. S3 경로: {}", s3Path);

                // 6. 재요약 허용 → 항상 새로 저장
                AudioTranscript newTranscript = new AudioTranscript();
                newTranscript.setVideo(video);
                newTranscript.setYoutubeId(youtubeId);
                newTranscript.setTranscriptPath(s3Path);
                newTranscript.setCreatedAt(LocalDateTime.now());

                audioTranscriptRepository.save(newTranscript);
                log.info("💾 Transcript 저장 완료: {}", newTranscript.getId());

            } else {
                log.error("❌ Whisper 서버 응답 오류: Status={}, Body={}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Whisper 서버 응답 실패: " + response.getStatusCode());
            }

        } catch (IllegalArgumentException e) {
            log.warn("❗ 잘못된 유튜브 URL 입력: {}", url);
            throw e;

        } catch (Exception e) {
            log.error("🔥 Whisper 처리 중 예외 발생", e);
            throw new RuntimeException("Whisper 처리 실패", e);
        }
    }
}
