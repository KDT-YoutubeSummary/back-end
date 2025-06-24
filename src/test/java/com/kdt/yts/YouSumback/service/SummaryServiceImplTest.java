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

        // NullPointerException 방지를 위해 DTO 생성에 필요한 모든 필드를 채워줍니다.
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
        // given: 테스트에 필요한 모든 전제 조건을 설정합니다.
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(audioTranscriptRepository.findByVideo_OriginalUrl(anyString())).thenReturn(Optional.of(testTranscript));
        when(tagRepository.findByTagName(anyString())).thenReturn(Optional.empty());

        // NullPointerException 방지를 위해 save 메소드가 호출될 때,
        // ID와 생성시간, 그리고 내부에 포함된 객체까지 명확하게 설정하여 반환합니다.
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> {
            Summary summary = invocation.getArgument(0);
            summary.setId(100L);
            summary.setCreatedAt(LocalDateTime.now());
            // 서비스 코드에서 설정한 transcript 객체를 그대로 유지하도록 보장합니다.
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

        when(openAIClient.chat(contains("요약 대상 내용"))).thenReturn(Mono.just("Test summary text."));
        when(openAIClient.chat(contains("핵심 해시태그"))).thenReturn(Mono.just("tag1, tag2, tag3"));

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.readString(any(Path.class), any(Charset.class)))
                    .thenReturn("This is a long transcript text from mock...");

            // when: 테스트하려는 실제 메소드를 호출합니다.
            SummaryResponseDTO response = summaryService.summarize(testRequest, 1L);

            // then: 메소드 호출 후 결과가 우리가 예상한 대로인지 검증합니다.
            assertNotNull(response, "응답 DTO는 null이 아니어야 합니다.");
            assertEquals("Test summary text.", response.getSummary());
            assertEquals(List.of("tag1", "tag2", "tag3"), response.getTags());
            assertEquals(100L, response.getSummaryId());
            assertNotNull(response.getCreatedAt(), "생성 시간은 null이 아니어야 합니다.");

            // ArgumentCaptor를 사용하여 save 메소드에 전달된 객체의 내용을 검증합니다.
            ArgumentCaptor<Summary> summaryCaptor = ArgumentCaptor.forClass(Summary.class);
            verify(summaryRepository, times(1)).save(summaryCaptor.capture());
            // 이 검증 라인에서 NPE가 발생하지 않도록, 위의 when()에서 내부 객체까지 모두 설정했습니다.
            assertEquals("test title", summaryCaptor.getValue().getAudioTranscript().getVideo().getTitle());

            ArgumentCaptor<SummaryArchive> archiveCaptor = ArgumentCaptor.forClass(SummaryArchive.class);
            verify(summaryArchiveRepository, times(1)).save(archiveCaptor.capture());
            assertEquals(testUser, archiveCaptor.getValue().getUser());
            assertNotNull(archiveCaptor.getValue().getId(), "저장된 Archive 객체는 ID를 가져야 합니다.");

            verify(tagRepository, times(3)).save(any(Tag.class));
            verify(summaryArchiveTagRepository, times(3)).save(any(SummaryArchiveTag.class));

            ArgumentCaptor<UserActivityLog> logCaptor = ArgumentCaptor.forClass(UserActivityLog.class);
            verify(userActivityLogRepository, times(1)).save(logCaptor.capture());
            assertEquals(testUser, logCaptor.getValue().getUser());
        }
    }
}