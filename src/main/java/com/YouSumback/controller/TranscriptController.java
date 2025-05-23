package com.YouSumback.controller;

import com.YouSumback.model.dto.request.YoutubeUrlRequest;
import com.YouSumback.model.entity.Video;
import com.YouSumback.model.entity.AudioTranscript;
import com.YouSumback.repository.VideoRepository;
import com.YouSumback.repository.AudioTranscriptRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Optional;

import org.json.JSONObject;

@RestController
@RequestMapping("/api")
public class TranscriptController {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private AudioTranscriptRepository audioTranscriptRepository;

    @PostMapping("/extract")
    public ResponseEntity<String> extractAndSaveTranscript(@RequestBody YoutubeUrlRequest request) {
        String youtubeUrl = request.getUrl();

        try {
            // 1. yt_whisper.py 실행
            ProcessBuilder builder = new ProcessBuilder("python3", "yt_whisper.py", youtubeUrl);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder outputBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                outputBuilder.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return ResponseEntity.status(500).body("Python 실행 실패");
            }

            // 2. 결과 파싱
            JSONObject result = new JSONObject(outputBuilder.toString());
            String videoId = result.getString("videoId");
            String title = result.getString("title");
            String originalUrl = result.getString("originalUrl");
            int durationSeconds = result.getInt("duration");
            String uploaderName = result.optString("uploaderName", "");
            String transcriptText = result.getString("transcript");

            // 3. Video 엔티티 저장 or 조회
            Video video = videoRepository.findById(videoId).orElseGet(() -> {
                Video newVideo = new Video();
                newVideo.setVideoId(videoId);
                newVideo.setTitle(title);
                newVideo.setOriginalUrl(originalUrl);
                newVideo.setUploaderName(uploaderName);
                newVideo.setOriginalLanguageCode("unknown");
                newVideo.setDurationSeconds(durationSeconds);
                return videoRepository.save(newVideo);
            });

            // 4. AudioTranscript 저장
            AudioTranscript transcript = new AudioTranscript();
            transcript.setTranscriptText(transcriptText);
            transcript.setCreateAt(LocalDateTime.now());
            transcript.setVideo(video);
            audioTranscriptRepository.save(transcript);

            return ResponseEntity.ok("Transcript 저장 완료");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("오류 발생: " + e.getMessage());
        }
    }
}
