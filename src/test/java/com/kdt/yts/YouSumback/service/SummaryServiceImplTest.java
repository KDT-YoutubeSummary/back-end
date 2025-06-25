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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        testRequest = new SummaryRequestDTO("http://youtu.be/test-id", "Test Prompt", SummaryType.BASIC);
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
            if (summary.getId() == null) summary.setId(100L);
            if (summary.getCreatedAt() == null) summary.setCreatedAt(LocalDateTime.now());
            if (summary.getAudioTranscript() == null) summary.setAudioTranscript(testTranscript);
            return summary;
        });

        when(summaryArchiveRepository.save(any(SummaryArchive.class))).thenAnswer(invocation -> {
            SummaryArchive archive = invocation.getArgument(0);
            if (archive.getId() == null) archive.setId(200L);
            return archive;
        });

        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            if (tag.getId() == null) tag.setId(new Random().nextLong());
            return tag;
        });

        when(summaryArchiveTagRepository.save(any(SummaryArchiveTag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userActivityLogRepository.save(any(UserActivityLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(openAIClient.chat(anyString())).thenReturn(Mono.just("Test summary text."));

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class)) {

            Path mockPath = mock(Path.class);
            mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            mockedPaths.when(() -> Paths.get(anyString(), anyString(), anyString())).thenReturn(mockPath);
            mockedFiles.when(() -> Files.readString(any(Path.class), any(Charset.class)))
                    .thenReturn("This is a long transcript text from mock...");

            // when
            SummaryResponseDTO response = summaryService.summarize(testRequest, 1L);

            // then
            assertNotNull(response, "응답 DTO는 null이 아니어야 합니다.");
            assertNotNull(response.getSummary(), "요약 내용은 null일 수 없습니다.");

            ArgumentCaptor<Summary> summaryCaptor = ArgumentCaptor.forClass(Summary.class);
            verify(summaryRepository, times(1)).save(summaryCaptor.capture());

            Summary capturedSummary = summaryCaptor.getValue();
            assertNotNull(capturedSummary, "저장된 Summary 객체는 null일 수 없습니다.");

            AudioTranscript capturedTranscript = capturedSummary.getAudioTranscript();
            assertNotNull(capturedTranscript, "Summary 객체 내부의 AudioTranscript는 null일 수 없습니다.");

            Video capturedVideo = capturedTranscript.getVideo();
            assertNotNull(capturedVideo, "AudioTranscript 내부의 Video 객체는 null일 수 없습니다.");

            assertEquals("test title", capturedVideo.getTitle());
        }
    }
}