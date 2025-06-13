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
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final VideoRepository videoRepository;
    private final AudioTranscriptRepository transcriptRepository;
    private final TextCleaner textCleaner;

    public Long extractYoutubeIdAndRunWhisper(String originalUrl, String purpose) throws Exception {
        String youtubeId = extractYoutubeId(originalUrl);
        if (youtubeId == null || youtubeId.isEmpty()) {
            throw new IllegalArgumentException("유효한 YouTube URL이 아닙니다.");
        }

        Video video = videoRepository.findByYoutubeId(youtubeId).orElseGet(() -> {
            Video newVideo = new Video();
            newVideo.setYoutubeId(youtubeId);
            newVideo.setOriginalUrl(originalUrl);
            newVideo.setTitle("제목 없음");
            newVideo.setUploaderName("unknown");
            newVideo.setPublishedAt(LocalDateTime.now());
            return videoRepository.save(newVideo);
        });

        List<String> command = List.of("python", "yt/yt_whisper.py", originalUrl);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(".")); // 현재 경로
        pb.redirectErrorStream(true);

        Process process = pb.start();
        int durationSeconds = -1;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[WHISPER STDOUT] " + line);
                if (line.startsWith("[DURATION_RESULT]")) {
                    try {
                        durationSeconds = Integer.parseInt(line.replace("[DURATION_RESULT]", "").trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("❌ Whisper 실행 실패 (exit code: " + exitCode + ")");
        }

        video.setDurationSeconds(durationSeconds);
        videoRepository.save(video);

        Path whisperTxtPath = Path.of("yt/textfiles/" + youtubeId + ".txt");
        Path vttPath = Path.of("yt/textfiles/" + youtubeId + ".ko.vtt");
        String rawFilePath;
        boolean isWhisper = false;

        if (Files.exists(whisperTxtPath)) {
            rawFilePath = whisperTxtPath.toString();
            isWhisper = true;
        } else if (Files.exists(vttPath)) {
            rawFilePath = vttPath.toString();
        } else {
            throw new FileNotFoundException("자막(.vtt) 또는 Whisper 결과(.txt) 파일이 존재하지 않습니다.");
        }

        String rawText;
        if (isWhisper) {
            rawText = Files.readString(Path.of(rawFilePath));
        } else {
            List<String> lines = Files.readAllLines(Path.of(rawFilePath));
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                if (line.matches("^[0-9]+$")) continue;
                if (line.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} --> .*")) continue;
                if (line.toLowerCase().contains("webvtt")) continue;
                if (line.matches(".*<\\d{2}:\\d{2}:\\d{2}\\.\\d{3}>.*")) continue;
                sb.append(line.trim()).append(" ");
            }
            rawText = sb.toString().trim();
        }

        String cleanedText = textCleaner.clean(rawText);
        String cleanedFilePath = "yt/textfiles/cleaned_" + youtubeId + ".txt";
        Files.writeString(Path.of(cleanedFilePath), cleanedText);

        AudioTranscript transcript = transcriptRepository.findByVideoId(video.getId())
                .map(existing -> {
                    existing.setTranscriptPath(cleanedFilePath);
                    existing.setCreatedAt(LocalDateTime.now());
                    return existing;
                })
                .orElseGet(() -> AudioTranscript.builder()
                        .video(video)
                        .youtubeId(youtubeId)
                        .transcriptPath(cleanedFilePath)
                        .createdAt(LocalDateTime.now())
                        .build()
                );

        transcriptRepository.save(transcript);
        return video.getId();
    }

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

    public String readTranscriptText(Long videoId) throws IOException {
        AudioTranscript transcript = transcriptRepository.findByVideoId(videoId)
                .orElseThrow(() -> new NoSuchElementException("Transcript not found"));
        return Files.readString(Path.of(transcript.getTranscriptPath()));
    }
}
