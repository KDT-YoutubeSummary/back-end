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

    // @InjectMocks: 테스트 대상 클래스입니다. @Mock으로 만든 가짜 객체들이 여기에 주입됩니다.
    @InjectMocks
    private SummaryServiceImpl summaryService;

    // @Mock: 가짜(Mock) 객체를 만듭니다. 실제 DB나 API와 통신하지 않습니다.
    @Mock private OpenAIClient openAIClient;
    @Mock private AudioTranscriptRepository audioTranscriptRepository;
    @Mock private SummaryRepository summaryRepository;
    @Mock private UserRepository userRepository;
    @Mock private TagRepository tagRepository;
    @Mock private SummaryArchiveRepository summaryArchiveRepository;
    @Mock private SummaryArchiveTagRepository summaryArchiveTagRepository;
    @Mock private UserActivityLogRepository userActivityLogRepository;

    // 테스트에서 사용할 공용 객체들
    private User testUser;
    private Video testVideo;
    private AudioTranscript testTranscript;
    private SummaryRequestDTO testRequest;

    @BeforeEach
    void setUp() {
        // 각 테스트가 실행되기 전에 필요한 객체들을 미리 생성합니다.

        testUser = User.builder().id(1L).userName("testuser").build();

        // 이 객체는 `summarize` 메소드 마지막 `return new SummaryResponseDTO(...)` 부분에서 사용됩니다.
        // 여기서 필드 하나라도 빠지면, 해당 DTO를 생성하거나 사용할 때 NullPointerException이 발생할 수 있습니다.
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

        // DB 조회 Mocking: DB에서 데이터를 찾아온 것처럼 행동하게 만듭니다.
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(audioTranscriptRepository.findByVideo_OriginalUrl(anyString())).thenReturn(Optional.of(testTranscript));
        when(tagRepository.findByTagName(anyString())).thenReturn(Optional.empty());

        // DB 저장 Mocking: `save` 메소드는 보통 DB에 저장된 후의 객체를 반환합니다.
        // 이 객체에는 DB가 생성해준 ID가 포함되어 있어야 합니다. 이 과정을 흉내 내는 것입니다.
        // 이 ID가 없으면, 이후 로직에서 `someObject.getId()` 호출 시 NullPointerException이 발생합니다.
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> {
            Summary summary = invocation.getArgument(0);
            summary.setId(100L); // ID 설정
            summary.setCreatedAt(LocalDateTime.now()); // 생성 시간 설정
            return summary;
        });

        when(summaryArchiveRepository.save(any(SummaryArchive.class))).thenAnswer(invocation -> {
            SummaryArchive archive = invocation.getArgument(0);
            archive.setId(200L); // ID 설정
            return archive;
        });

        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(new Random().nextLong()); // ID 설정
            return tag;
        });

        // 나머지 save 메소드들도 null을 반환하지 않도록 설정합니다.
        when(summaryArchiveTagRepository.save(any(SummaryArchiveTag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userActivityLogRepository.save(any(UserActivityLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 외부 API(OpenAI) 호출 Mocking
        when(openAIClient.chat(contains("요약 대상 내용"))).thenReturn(Mono.just("Test summary text."));
        when(openAIClient.chat(contains("핵심 해시태그"))).thenReturn(Mono.just("tag1, tag2, tag3"));

        // 정적(static) 메소드 Mocking
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.readString(any(Path.class))).thenReturn("This is a long transcript text...");

            // when: 테스트하려는 실제 메소드를 호출합니다.
            SummaryResponseDTO response = summaryService.summarize(testRequest, 1L);

            // then: 메소드 호출 후 결과가 우리가 예상한 대로인지 검증합니다.
            assertNotNull(response, "응답 DTO는 null이 아니어야 합니다.");
            assertEquals("Test summary text.", response.getSummary());
            assertEquals(List.of("tag1", "tag2", "tag3"), response.getTags());
            assertEquals(100L, response.getSummaryId());
            assertNotNull(response.getCreatedAt(), "생성 시간은 null이 아니어야 합니다.");

            // ArgumentCaptor: `save` 메소드에 어떤 객체가 전달되었는지 붙잡아서 내용을 검증합니다.
            ArgumentCaptor<Summary> summaryCaptor = ArgumentCaptor.forClass(Summary.class);
            verify(summaryRepository, times(1)).save(summaryCaptor.capture());
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
