package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.Util.TextCleaner;
import com.kdt.yts.YouSumback.model.entity.AudioTranscript;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.AudioTranscriptRepository;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final VideoRepository videoRepository;
    private final AudioTranscriptRepository transcriptRepository;
    private final TextCleaner textCleaner;

    // [수정] AWS S3 클라이언트 주입
    private final S3Client s3Client;

    // [수정] application.properties 등에서 S3 버킷 이름 주입
    @Value("${aws.s3.bucket-name}")
    private String s3BucketName;


    public Long extractYoutubeIdAndRunWhisper(String originalUrl, String purpose) throws Exception {
        // 1. 유튜브 ID 추출
        String youtubeId = extractYoutubeId(originalUrl);
        if (youtubeId == null || youtubeId.isEmpty()) {
            throw new IllegalArgumentException("유효한 YouTube URL이 아닙니다.");
        }

        // 2. Video 엔티티 없으면 자동 등록
        Video video = videoRepository.findByYoutubeId(youtubeId).orElseGet(() -> {
            Video newVideo = new Video();
            newVideo.setYoutubeId(youtubeId);
            newVideo.setOriginalUrl(originalUrl);
            newVideo.setTitle("제목 없음");
            newVideo.setUploaderName("unknown");
            newVideo.setPublishedAt(LocalDateTime.now());
            return videoRepository.save(newVideo);
        });

        // 3. Whisper Python 스크립트 실행
        // [수정] 스크립트 경로를 EC2 서버의 절대 경로로 지정하는 것이 더 안정적입니다.
        String scriptPath = "/home/ec2-user/your-project-dir/yt_whisper.py"; // 실제 경로로 수정
        List<String> command = List.of("python3", scriptPath, originalUrl);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // [수정] Python 스크립트로부터 정보를 수신할 변수들
        int durationSeconds = -1;
        String s3Path = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[PYTHON STDOUT] " + line);
                if (line.startsWith("[DURATION_RESULT]")) {
                    durationSeconds = Integer.parseInt(line.replace("[DURATION_RESULT]", "").trim());
                } else if (line.startsWith("[S3_PATH_RESULT]")) {
                    s3Path = line.replace("[S3_PATH_RESULT]", "").trim();
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0 || s3Path == null) {
            throw new RuntimeException("❌ Python 스크립트 실행 실패 또는 S3 경로 수신 실패 (exit code: " + exitCode + ")");
        }

        video.setDurationSeconds(durationSeconds);
        videoRepository.save(video);

        // [수정] 4. S3에서 파일 내용 읽기
        String rawText = getObjectContentFromS3(s3Path);

        // [수정] 5. 파일 타입에 따른 정제 처리 (로직은 거의 동일, 대상이 로컬 파일 -> S3에서 읽어온 텍스트)
        boolean isVtt = s3Path.endsWith(".vtt");
        if (isVtt) {
            StringBuilder sb = new StringBuilder();
            for (String line : rawText.split("\n")) {
                if (line.trim().isEmpty() || line.matches("^[0-9]+$") || line.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} --> .*") || line.toLowerCase().contains("webvtt") || line.matches(".*<\\d{2}:\\d{2}:\\d{2}\\.\\d{3}>.*")) continue;
                sb.append(line.trim()).append(" ");
            }
            rawText = sb.toString().trim();
        }

        // 6. 텍스트 정제 및 DB 저장
        String cleanedText = textCleaner.clean(rawText);
        // [수정] 정제된 텍스트를 로컬에 저장하지 않고, 필요하다면 S3에 다시 업로드하거나 바로 다음 단계로 넘김
        // 여기서는 AudioTranscript 엔티티에 정제된 텍스트 자체를 저장하거나, S3 경로를 저장하는 방식을 선택할 수 있습니다.
        // 여기서는 원본 S3 경로를 저장하는 것으로 가정합니다.

        // 7. DB에 경로 저장
        final String finalS3Path = s3Path;
        AudioTranscript transcript = transcriptRepository.findByVideoId(video.getId())
                .map(existing -> {
                    existing.setTranscriptPath(finalS3Path); // S3 경로 저장
                    existing.setCreatedAt(LocalDateTime.now());
                    return existing;
                })
                .orElseGet(() -> AudioTranscript.builder()
                        .video(video)
                        .youtubeId(youtubeId)
                        .transcriptPath(finalS3Path) // S3 경로 저장
                        .createdAt(LocalDateTime.now())
                        .build()
                );
        transcriptRepository.save(transcript);

        return video.getId();
    }

    // [수정] S3에서 객체 내용을 문자열로 읽어오는 헬퍼 메소드 추가
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

    // 유튜브 URL에서 ID 추출 (기존 코드 유지)
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
            System.err.println("YouTube ID 추출 중 예외 발생: " + e.getMessage());
            return null;
        }
        return null;
    }

    // [수정] S3 경로를 이용해 텍스트 읽기
    public String readTranscriptText(Long videoId) throws IOException {
        AudioTranscript transcript = transcriptRepository.findByVideoId(videoId)
                .orElseThrow(() -> new NoSuchElementException("Transcript not found"));

        // transcriptPath가 이제 S3 key이므로, S3에서 내용을 읽어와야 합니다.
        return getObjectContentFromS3(transcript.getTranscriptPath());
    }
}