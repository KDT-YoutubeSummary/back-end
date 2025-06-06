package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.Util.TextCleaner;
import com.kdt.yts.YouSumback.model.entity.AudioTranscript;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.AudioTranscriptRepository;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final VideoRepository videoRepository;
    private final AudioTranscriptRepository transcriptRepository;
    private final TextCleaner textCleaner;

    public void extractYoutubeIdAndRunWhisper(String originalUrl, String purpose) throws Exception {
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
        List<String> command = List.of(
                "python",
                "yt/yt_whisper.py",
                originalUrl  // ← 전체 URL
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(".")); // 현재 디렉토리 기준
        pb.redirectErrorStream(false); // stderr 분리

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[WHISPER STDOUT] " + line);
            }
        }

        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                System.err.println("[WHISPER STDERR] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("❌ Whisper 실행 실패 (exit code: " + exitCode + ")");
        }
        // 4. Whisper 텍스트 파일 읽고 정제
        String transcriptPath = "src/main/resources/textfiles/" + youtubeId + ".txt";
        String rawText = Files.readString(Path.of(transcriptPath));
        String cleanedText = textCleaner.clean(rawText);

        // 5. AudioTranscript 저장 전 중복 확인
        if (transcriptRepository.findByVideoId(video.getId()).isPresent()) {
            System.out.println("📌 이미 해당 영상에 대한 transcript가 존재합니다. 저장 생략.");
            return;
        }

        // 저장
        AudioTranscript transcript = AudioTranscript.builder()
                .video(video)
                .youtubeId(youtubeId)
                .transcriptText(cleanedText)
                .createdAt(LocalDateTime.now())
                .build();
        transcriptRepository.save(transcript);
    }

    // 유튜브 URL에서 ID 추출
    private String extractYoutubeId(String url) {
        try {
            if (url.contains("v=")) {
                String[] parts = url.split("v=");
                String afterV = parts[1];
                return afterV.contains("&") ? afterV.split("&")[0] : afterV;
            } else if (url.contains("youtu.be/")) {
                return url.substring(url.lastIndexOf("/") + 1);
            }
        } catch (Exception e) {
            throw new RuntimeException("YouTube ID 추출 실패", e);
        }
        return null;
    }
}
