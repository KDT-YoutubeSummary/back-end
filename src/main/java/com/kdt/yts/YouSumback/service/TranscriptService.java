package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.TranscriptSaveRequestDto;
import com.kdt.yts.YouSumback.model.dto.response.TranscriptSaveResponseDto;
import com.kdt.yts.YouSumback.model.dto.response.TranscriptLookupResponseDto;
import com.kdt.yts.YouSumback.model.entity.AudioTranscript;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.AudioTranscriptRepository;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final VideoRepository videoRepository;
    private final AudioTranscriptRepository transcriptRepository;
    private final YouTubeMetadataService youTubeMetadataService;

    // ✅ STT 저장 (url 기반 요청 → youtubeId 추출 → 메타데이터 저장 → whisper)
    public TranscriptSaveResponseDto saveTranscript(TranscriptSaveRequestDto requestDto) throws Exception {
        String url = requestDto.getUrl();
        String youtubeId = youTubeMetadataService.extractYoutubeId(url);

        // 1. 메타데이터 저장 (이미 저장된 경우 무시)
        youTubeMetadataService.saveVideoMetadata(youtubeId);

        // 2. 영상 정보 조회
        Video video = videoRepository.findByYoutubeId(youtubeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 영상 정보가 존재하지 않습니다."));

        String youtubeUrl = video.getOriginalUrl();

        // 3. STT 처리
        runCommand("yt-dlp -x --audio-format wav -o \"" + youtubeId + ".%(ext)s\" " + youtubeUrl);
        runCommand("whisper " + youtubeId + ".wav --model medium --output_format txt");

        String transcriptText = readAndCleanTextFile(youtubeId + ".txt");

        // 4. DB 저장
        AudioTranscript transcript = AudioTranscript.builder()
                .video(video)
                .transcriptText(transcriptText)
                .createdAt(LocalDateTime.now())
                .build();

        AudioTranscript saved = transcriptRepository.save(transcript);

        return TranscriptSaveResponseDto.builder()
                .transcriptId(saved.getId())
                .createAt(saved.getCreatedAt().toString())
                .build();
    }

    public List<TranscriptLookupResponseDto> getTranscriptListByYoutubeId(String youtubeId) {
        Video video = videoRepository.findByYoutubeId(youtubeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 영상입니다."));

        return transcriptRepository.findByYoutubeId(youtubeId).stream()
                .map(t -> new TranscriptLookupResponseDto(
                        t.getId(),
                        video.getId(),
                        t.getCreatedAt().toString(),
                        t.getTranscriptText()
                ))
                .collect(Collectors.toList());
    }

    public void deleteTranscript(Long transcriptId) {
        if (!transcriptRepository.existsById(transcriptId)) {
            throw new IllegalArgumentException("존재하지 않는 텍스트입니다.");
        }
        transcriptRepository.deleteById(transcriptId);
    }

    private String readAndCleanTextFile(String filePath) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String cleaned = line.trim();
                if (!cleaned.isEmpty()) {
                    sb.append(cleaned).append(" ");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("텍스트 파일 읽기 실패: " + filePath, e);
        }
        return sb.toString().trim();
    }

    private void runCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[CMD] " + line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("명령어 실패: " + command);
            }
        } catch (Exception e) {
            throw new RuntimeException("명령어 실행 중 오류 발생: " + command, e);
        }
    }
}
