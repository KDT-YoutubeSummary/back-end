package com.kdt.yts.YouSumback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.yts.YouSumback.model.entity.AudioTranscript;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.AudioTranscriptRepository;
import com.kdt.yts.YouSumback.repository.VideoRepository;
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
    private final YouTubeMetadataService youTubeMetadataService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Long extractYoutubeIdAndRunWhisper(String url, String userPrompt) throws Exception {
        String youtubeId = youTubeMetadataService.extractYoutubeId(url);

        Video video = videoRepository.findByYoutubeId(youtubeId)
                .orElseThrow(() -> new RuntimeException("Video not found for YouTube ID: " + youtubeId));

        // 이미 처리된 transcript가 있는지 확인
        Optional<AudioTranscript> existingTranscript = audioTranscriptRepository.findByVideoId(video.getId());
        if (existingTranscript.isPresent() && 
            ((existingTranscript.get().getTranscriptPath() != null && !existingTranscript.get().getTranscriptPath().isEmpty()) ||
             (existingTranscript.get().getTranscriptText() != null && !existingTranscript.get().getTranscriptText().isEmpty()))) {
            System.out.println("이미 처리된 Transcript가 존재합니다. ID: " + existingTranscript.get().getId());
            return video.getId();
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

                /*
                 * ===============================
                 * 기존 S3 방식 - 주석처리됨
                 * 로컬 파일 시스템 저장 방식으로 변경
                 * ===============================
                 */
                /*
                // ⭐️⭐️⭐️ [핵심 수정] whisper-server의 응답에서 올바른 s3_path를 파싱하여 사용합니다. ⭐️⭐️⭐️
                String s3Path = responseBody.path("s3_path").asText();
                if (s3Path == null || s3Path.isEmpty()) {
                    throw new RuntimeException("Whisper 서버가 S3 경로를 반환하지 않았습니다.");
                }
                System.out.println("✅ Whisper 서버로부터 받은 S3 경로: " + s3Path);
                */

                // ⭐️⭐️⭐️ [새로운 방식] Whisper 서버에서 텍스트를 받아서 로컬 파일로 저장 ⭐️⭐️⭐️
                String transcriptText = responseBody.path("transcript_text").asText();
                if (transcriptText == null || transcriptText.isEmpty()) {
                    throw new RuntimeException("Whisper 서버가 텍스트를 반환하지 않았습니다.");
                }
                System.out.println("✅ Whisper 서버로부터 받은 텍스트 길이: " + transcriptText.length() + " characters");

                // 로컬 파일 시스템에 텍스트 저장
                String localFilePath = saveTextToLocalFile(youtubeId, transcriptText);
                System.out.println("✅ 텍스트가 로컬 파일에 저장됨: " + localFilePath);

                // DB에 파일 경로 저장
                AudioTranscript transcript = existingTranscript.orElse(new AudioTranscript());
                transcript.setVideo(video);
                transcript.setYoutubeId(youtubeId);
                transcript.setTranscriptPath(localFilePath); // 로컬 파일 경로 저장
                
                // 텍스트도 DB에 백업으로 저장 (선택적)
                transcript.setTranscriptText(transcriptText);
                
                transcript.setCreatedAt(LocalDateTime.now());
                audioTranscriptRepository.save(transcript);

                System.out.println("✅ 파일 경로가 DB에 성공적으로 저장되었습니다. Transcript ID: " + transcript.getId());
                return video.getId();
            } else {
                throw new RuntimeException("Whisper 서버 처리 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("❌ Whisper 서버 통신 중 오류 발생: " + e.getMessage());
            throw new RuntimeException("Whisper 서버 통신 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 텍스트를 로컬 파일 시스템에 저장하고 경로를 반환
     */
    private String saveTextToLocalFile(String youtubeId, String text) {
        try {
            // 텍스트 파일 저장 디렉토리 설정
            String baseDir = "src/main/resources/textfiles";
            java.nio.file.Path dirPath = java.nio.file.Paths.get(baseDir);
            
            // 디렉토리가 없으면 생성
            if (!java.nio.file.Files.exists(dirPath)) {
                java.nio.file.Files.createDirectories(dirPath);
            }
            
            // 파일명 생성 (cleaned_youtubeId.txt)
            String fileName = "cleaned_" + youtubeId + ".txt";
            java.nio.file.Path filePath = dirPath.resolve(fileName);
            
            // 텍스트 파일로 저장
            java.nio.file.Files.writeString(filePath, text, java.nio.charset.StandardCharsets.UTF_8);
            
            // 상대 경로 반환
            return filePath.toString();
            
        } catch (Exception e) {
            System.err.println("❌ 로컬 파일 저장 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save text to local file", e);
        }
    }
}

