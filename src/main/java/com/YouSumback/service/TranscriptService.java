package com.YouSumback.service;

import com.YouSumback.model.dto.request.TranscriptSaveRequestDto;
import com.YouSumback.model.dto.response.TranscriptLookupResponseDto;
import com.YouSumback.model.dto.response.TranscriptSaveResponseDto;
import com.YouSumback.model.entity.TranscriptText;
import com.YouSumback.model.entity.Video;
import com.YouSumback.repository.TranscriptRepository;
import com.YouSumback.repository.VideoRepository;
import com.YouSumback.util.TextCleaner;
import com.YouSumback.util.WhisperRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TranscriptService {

    @Autowired
    private WhisperRunner whisperRunner;

    @Autowired
    private TextCleaner textCleaner;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private TranscriptRepository transcriptRepository;

    public TranscriptSaveResponseDto saveTranscript(TranscriptSaveRequestDto requestDto) {
        // 1. 영상 조회
        Video video = videoRepository.findById(requestDto.getVideoId())
                .orElseThrow(() -> new IllegalArgumentException("해당 영상이 존재하지 않습니다."));

        // 2. Whisper 실행 + 텍스트 정제
        String extractedText;
        try {
            extractedText = whisperRunner.runWhisper(video.getVideoUrl());
            // whisper 실행 로직
        } catch (Exception e) {
            throw new RuntimeException("Whisper 실행 실패: " + e.getMessage(), e);
        }

        String cleanedText = textCleaner.clean(extractedText);

        // 3. 저장
        TranscriptText transcript = new TranscriptText();
        transcript.setVideo(video);
        transcript.setTranscriptText(cleanedText);
        transcript.setCreatedAt(LocalDateTime.now());

        transcriptRepository.save(transcript);

        return new TranscriptSaveResponseDto(transcript.getId(), transcript.getCreatedAt().toString());
    }

    public List<TranscriptLookupResponseDto> getTranscriptListByVideoId(String videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 영상입니다."));

        return transcriptRepository.findByVideo(video).stream()
                .map(t -> new TranscriptLookupResponseDto(
                        t.getId(),
                        videoId,
                        t.getCreatedAt().toString(),
                        t.getTranscriptText()
                )).collect(Collectors.toList());

    }


    public void deleteTranscript(Long transcriptId) {
        if (!transcriptRepository.existsById(transcriptId)) {
            throw new IllegalArgumentException("존재하지 않는 텍스트입니다.");
        }
        transcriptRepository.deleteById(transcriptId);
    }

}
