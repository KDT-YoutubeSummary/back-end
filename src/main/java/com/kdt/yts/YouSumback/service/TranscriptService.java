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
import java.util.HashMap;
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
    public void extractYoutubeIdAndRunWhisper(String url, String userPrompt) {
        try {
            // 1. 유튜브 ID 추출
            String youtubeId = metadataHelper.extractYoutubeId(url);
            System.out.println("📺 유튜브 ID 추출: " + youtubeId);

            // 2. 해당 Video 조회
            Video video = videoRepository.findByYoutubeId(youtubeId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 YouTube ID에 해당하는 영상 없음: " + youtubeId));

            // 3. 기존 Transcript 있는지 확인
            Optional<AudioTranscript> optionalTranscript = audioTranscriptRepository.findByVideoId(video.getId());
            if (optionalTranscript.isPresent() && optionalTranscript.get().getTranscriptPath() != null) {
                System.out.println("🔁 이미 처리된 Transcript 존재. ID: " + optionalTranscript.get().getId());
                return;
            }

            // 4. Whisper 서버 호출 준비
            String whisperServerUrl = "http://whisper-server:8000/transcribe";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("videoUrl", url);
            requestBody.put("youtubeId", youtubeId);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            System.out.println("📤 Whisper 서버로 요청 전송...");
            ResponseEntity<String> response = restTemplate.postForEntity(whisperServerUrl, request, String.class);

            // 5. 응답 처리
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                String s3Path = json.path("s3_path").asText();

                if (s3Path == null || s3Path.isEmpty()) {
                    throw new IllegalStateException("❌ Whisper 응답에 s3_path 없음");
                }

                System.out.println("✅ Whisper 처리 성공. S3 경로: " + s3Path);

                // 6. 새 Transcript 저장 또는 업데이트
                AudioTranscript transcript = optionalTranscript.orElse(new AudioTranscript());
                transcript.setVideo(video);
                transcript.setYoutubeId(youtubeId);
                transcript.setTranscriptPath(s3Path);
                transcript.setCreatedAt(LocalDateTime.now());

                audioTranscriptRepository.save(transcript);
                System.out.println("💾 Transcript 저장 완료");

            } else {
                System.err.println("❌ Whisper 서버 오류 응답: " + response.getStatusCode());
                System.err.println("내용: " + response.getBody());
                throw new RuntimeException("Whisper 서버 응답 실패");
            }

        } catch (Exception e) {
            System.err.println("❌ Whisper 처리 중 예외 발생");
            e.printStackTrace();
            throw new RuntimeException("Whisper 처리 실패", e);
        }


    }
}
