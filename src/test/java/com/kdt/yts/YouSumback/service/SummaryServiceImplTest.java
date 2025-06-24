package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import com.kdt.yts.YouSumback.service.client.OpenAIClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryServiceImplTest {

    @InjectMocks
    private SummaryServiceImpl summaryService;

    @Mock private OpenAIClient openAIClient;
    @Mock private AudioTranscriptRepository audioTranscriptRepository;
    @Mock private SummaryRepository summaryRepository;
    @Mock private UserRepository userRepository;
    @Mock private TagRepository tagRepository;
    @Mock private SummaryArchiveRepository summaryArchiveRepository;
    @Mock private SummaryArchiveTagRepository summaryArchiveTagRepository;
    @Mock private UserActivityLogRepository userActivityLogRepository;

    private User testUser;
    private Video testVideo;
    private AudioTranscript testTranscript;
    private SummaryRequestDTO testRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).userName("testuser").build();

        testVideo = Video.builder()
                .id(1L)
                .youtubeId("test-id")
                .title("test title")
                .thumbnailUrl("http://thumbnail.url/test.jpg")
                .uploaderName("Test Uploader")
                .viewCount(1000L)
                .originalLanguageCode("ko")
                .build();

        testTranscript = AudioTranscript.builder()
                .id(1L)
                .video(testVideo)
                .transcriptPath("dummy/path/cleaned_test-id.txt")
                .build();

        testRequest = new SummaryRequestDTO();
        testRequest.setOriginalUrl("http://youtu.be/test-id");
        testRequest.setUserPrompt("Test Prompt");
        testRequest.setSummaryType(SummaryType.BASIC);
    }

    @Test
    @DisplayName("요약 및 해시태그 생성 단위 테스트 - 성공 (FR-007, FR-008, FR-009)")
    void summarize_Success() throws IOException {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(audioTranscriptRepository.findByVideo_OriginalUrl(anyString())).thenReturn(Optional.of(testTranscript));
        when(tagRepository.findByTagName(anyString())).thenReturn(Optional.empty());

        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> {
            Summary summary = invocation.getArgument(0);
            summary.setId(100L);
            summary.setCreatedAt(LocalDateTime.now());
            summary.setAudioTranscript(testTranscript);
            return summary;
        });

        when(summaryArchiveRepository.save(any(SummaryArchive.class))).thenAnswer(invocation -> {
            SummaryArchive archive = invocation.getArgument(0);
            archive.setId(200L);
            return archive;
        });

        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(new Random().nextLong());
            return tag;
        });

        when(summaryArchiveTagRepository.save(any(SummaryArchiveTag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userActivityLogRepository.save(any(UserActivityLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(openAIClient.chat(contains("요약 대상 내용"))).thenReturn(Mono.just("부분 요약 내용."));
        when(openAIClient.chat(contains("하나로 합쳐서"))).thenReturn(Mono.just("Test summary text."));
        when(openAIClient.chat(contains("핵심 해시태그"))).thenReturn(Mono.just("tag1, tag2, tag3"));

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.readString(any(Path.class), any(Charset.class)))
                    .thenReturn("This is a long transcript text from mock...");

            // when
            SummaryResponseDTO response = summaryService.summarize(testRequest, 1L);

            // then
            assertNotNull(response, "응답 DTO는 null이 아니어야 합니다.");
            assertEquals("Test summary text.", response.getSummary());

            // ⭐️⭐️⭐️ 여기가 최종 디버깅 포인트입니다! ⭐️⭐️⭐️
            // 132번째 줄의 검증을 여러 단계로 나누어 정확한 원인을 찾습니다.
            ArgumentCaptor<Summary> summaryCaptor = ArgumentCaptor.forClass(Summary.class);
            verify(summaryRepository, times(1)).save(summaryCaptor.capture());

            // 1단계: save 메소드에 전달된 summary 객체 자체가 null인지 확인합니다.
            Summary capturedSummary = summaryCaptor.getValue();
            assertNotNull(capturedSummary, "저장된 Summary 객체는 null일 수 없습니다.");

            // 2단계: summary 객체 안의 transcript 객체가 null인지 확인합니다.
            AudioTranscript capturedTranscript = capturedSummary.getAudioTranscript();
            assertNotNull(capturedTranscript, "Summary 객체 내부의 AudioTranscript는 null일 수 없습니다.");

            // 3단계: transcript 객체 안의 video 객체가 null인지 확인합니다.
            Video capturedVideo = capturedTranscript.getVideo();
            assertNotNull(capturedVideo, "AudioTranscript 내부의 Video 객체는 null일 수 없습니다.");

            // 4단계: 모든 객체가 null이 아님을 확인했으므로, 최종적으로 제목을 비교합니다.
            assertEquals("test title", capturedVideo.getTitle());
        }
    }
}