package com.kdt.yts.YouSumback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.yts.YouSumback.model.entity.AudioTranscript;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.AudioTranscriptRepository;
import com.kdt.yts.YouSumback.util.MetadataHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final RestTemplate restTemplate;
    private final AudioTranscriptRepository audioTranscriptRepository;
    private final MetadataHelper metadataHelper;
    private final ObjectMapper objectMapper;

    @Transactional
    public void extractYoutubeIdAndRunWhisper(String url, String userPrompt) {
        try {
            // 1. 영상 메타데이터 확보 (없으면 생성)
            Video video = metadataHelper.fetchOrCreateMetadata(url);
            String youtubeId = video.getYoutubeId();
            log.info("📺 유튜브 ID 확보 및 메타데이터 준비 완료: {}", youtubeId);

            // 2. 기존 Transcript 확인 (재요약 허용이므로 무시하고 계속 진행)
            Optional<AudioTranscript> optionalTranscript = audioTranscriptRepository.findByVideoId(video.getId());

            // 3. Whisper 서버 호출 준비
            String whisperServerUrl = "http://whisper-server:8000/transcribe";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("videoUrl", url);
            requestBody.put("youtubeId", youtubeId);
            requestBody.put("userPrompt", userPrompt == null ? "" : userPrompt);  // 빈 문자열 허용

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            log.info("📤 Whisper 서버로 요청 전송 시작...");
            ResponseEntity<String> response = restTemplate.postForEntity(whisperServerUrl, request, String.class);

            // 4. 응답 처리
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                String s3Path = json.path("s3_path").asText();

                if (s3Path == null || s3Path.isBlank() || !s3Path.endsWith(".txt")) {
                    throw new IllegalStateException("❌ Whisper 응답에 유효한 s3_path 없음: " + s3Path);
                }

                log.info("✅ Whisper 처리 성공. S3 경로: {}", s3Path);

                // 5. Transcript 저장 (재요약 시 덮어쓰기)
                AudioTranscript transcript = optionalTranscript.orElse(new AudioTranscript());
                transcript.setVideo(video);
                transcript.setYoutubeId(youtubeId);
                transcript.setTranscriptPath(s3Path);
                transcript.setCreatedAt(LocalDateTime.now());

                audioTranscriptRepository.save(transcript);
                log.info("💾 Transcript 저장 완료: {}", transcript.getId());

            } else {
                log.error("❌ Whisper 서버 오류 응답: {}", response.getStatusCode());
                log.error("응답 내용: {}", response.getBody());
                throw new RuntimeException("Whisper 서버 응답 실패: " + response.getStatusCode() + " - " + response.getBody());
            }

        } catch (IllegalArgumentException e) {
            throw e; // 컨트롤러에서 처리하게 던짐

        } catch (Exception e) {
            log.error("❌ Whisper 처리 중 예외 발생", e);
            throw new RuntimeException("Whisper 처리 실패", e);
        }
    }
}
