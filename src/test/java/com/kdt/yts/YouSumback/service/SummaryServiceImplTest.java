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

    @Mock
    private OpenAIClient openAIClient;
    @Mock
    private AudioTranscriptRepository audioTranscriptRepository;
    @Mock
    private SummaryRepository summaryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private SummaryArchiveRepository summaryArchiveRepository;
    @Mock
    private SummaryArchiveTagRepository summaryArchiveTagRepository;
    @Mock
    private UserActivityLogRepository userActivityLogRepository;


    @InjectMocks
    private SummaryServiceImpl summaryService;

    private User testUser;
    private Video testVideo;
    private AudioTranscript testTranscript;
    private SummaryRequestDTO testRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).userName("testuser").build();

        // ⭐️⭐️⭐️ 핵심 수정 포인트 1: Video 객체에 모든 필드 값을 채워줍니다. ⭐️⭐️⭐️
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

        // ⭐️⭐️⭐️ 핵심 수정 포인트 2: save 동작 시 ID와 생성 시간을 모두 설정합니다. ⭐️⭐️⭐️
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> {
            Summary summary = invocation.getArgument(0);
            summary.setId(100L);
            // 서비스 코드에서 LocalDateTime.now()로 설정하지만, 테스트의 일관성을 위해 여기서도 명시적으로 설정
            if (summary.getCreatedAt() == null) {
                summary.setCreatedAt(LocalDateTime.now());
            }
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

        when(openAIClient.chat(contains("요약 대상 내용"))).thenReturn(Mono.just("Test summary text."));
        when(openAIClient.chat(contains("핵심 해시태그"))).thenReturn(Mono.just("tag1, tag2, tag3"));

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.readString(any(Path.class))).thenReturn("This is a long transcript text...");

            // when
            SummaryResponseDTO response = summaryService.summarize(testRequest, 1L);

            // then
            assertNotNull(response);
            assertEquals("Test summary text.", response.getSummary());
            assertEquals(List.of("tag1", "tag2", "tag3"), response.getTags());
            assertEquals(100L, response.getSummaryId());
            assertNotNull(response.getCreatedAt()); // 생성 시간도 null이 아닌지 확인

            ArgumentCaptor<Summary> summaryCaptor = ArgumentCaptor.forClass(Summary.class);
            verify(summaryRepository, times(1)).save(summaryCaptor.capture());
            assertEquals("test title", summaryCaptor.getValue().getAudioTranscript().getVideo().getTitle());

            ArgumentCaptor<SummaryArchive> archiveCaptor = ArgumentCaptor.forClass(SummaryArchive.class);
            verify(summaryArchiveRepository, times(1)).save(archiveCaptor.capture());
            assertEquals(testUser, archiveCaptor.getValue().getUser());
            assertNotNull(archiveCaptor.getValue().getId());

            verify(tagRepository, times(3)).save(any(Tag.class));
            verify(summaryArchiveTagRepository, times(3)).save(any(SummaryArchiveTag.class));

            ArgumentCaptor<UserActivityLog> logCaptor = ArgumentCaptor.forClass(UserActivityLog.class);
            verify(userActivityLogRepository, times(1)).save(logCaptor.capture());
            assertEquals(testUser, logCaptor.getValue().getUser());
        }
    }
}
