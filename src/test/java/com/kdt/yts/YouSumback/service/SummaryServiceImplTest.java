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
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

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
        testVideo = Video.builder().id(1L).youtubeId("test-id").title("test title").build();
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
        // given: 모든 외부 의존성(Repository, Client)이 어떻게 동작할지 정의합니다.

        // 1. DB 조회 관련 Mocking
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(audioTranscriptRepository.findByVideo_OriginalUrl(anyString())).thenReturn(Optional.of(testTranscript));
        when(tagRepository.findByTagName(anyString())).thenReturn(Optional.empty()); // 새로운 태그라고 가정

        // 2. DB 저장 관련 Mocking
        // save 메소드가 호출되면, 파라미터로 받은 객체를 그대로 반환하도록 설정합니다. (실제 DB처럼 동작)
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> {
            Summary summary = invocation.getArgument(0);
            summary.setId(100L); // 저장 후 ID가 생성된 것처럼 시뮬레이션
            return summary;
        });
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(summaryArchiveRepository.save(any(SummaryArchive.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(summaryArchiveTagRepository.save(any(SummaryArchiveTag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userActivityLogRepository.save(any(UserActivityLog.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // 3. OpenAI Client Mocking
        // 실제 프롬프트에 포함된 키워드로 더 정확하게 매칭합니다.
        when(openAIClient.chat(contains("요약 대상 내용"))).thenReturn(Mono.just("Test summary text."));
        // 해시태그 추출 프롬프트는 '해시태그'와 '기본 태그 목록' 이라는 키워드를 포함합니다.
        when(openAIClient.chat(contains("핵심 해시태그"))).thenReturn(Mono.just("tag1, tag2, tag3"));

        // 4. 정적(static) 메소드인 Files.readString Mocking
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.readString(any(Path.class))).thenReturn("This is a long transcript text...");

            // when: 실제 테스트하려는 메소드를 호출합니다.
            SummaryResponseDTO response = summaryService.summarize(testRequest, 1L);

            // then: 결과가 예상과 일치하는지 검증합니다.
            assertNotNull(response);
            assertEquals("Test summary text.", response.getSummary());
            assertEquals(List.of("tag1", "tag2", "tag3"), response.getTags());
            assertEquals(100L, response.getSummaryId()); // Mocking된 ID로 검증

            // ArgumentCaptor를 사용하여 save 메서드에 전달된 실제 객체를 캡처하여 내용을 검증합니다.
            ArgumentCaptor<Summary> summaryCaptor = ArgumentCaptor.forClass(Summary.class);
            verify(summaryRepository, times(1)).save(summaryCaptor.capture());
            Summary capturedSummary = summaryCaptor.getValue();
            assertEquals("Test summary text.", capturedSummary.getSummaryText());
            assertEquals(testUser, capturedSummary.getUser());

            ArgumentCaptor<SummaryArchive> archiveCaptor = ArgumentCaptor.forClass(SummaryArchive.class);
            verify(summaryArchiveRepository, times(1)).save(archiveCaptor.capture());
            assertEquals(testUser, archiveCaptor.getValue().getUser());

            // 총 3개의 태그가 생성(save)되고, 3개의 연결(SummaryArchiveTag)이 생성되는지 확인합니다.
            verify(tagRepository, times(3)).save(any(Tag.class));
            verify(summaryArchiveTagRepository, times(3)).save(any(SummaryArchiveTag.class));

            ArgumentCaptor<UserActivityLog> logCaptor = ArgumentCaptor.forClass(UserActivityLog.class);
            verify(userActivityLogRepository, times(1)).save(logCaptor.capture());
            assertEquals(testUser, logCaptor.getValue().getUser());
        }
    }
}