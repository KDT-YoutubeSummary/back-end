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
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(audioTranscriptRepository.findByVideo_OriginalUrl(anyString())).thenReturn(Optional.of(testTranscript));

        // when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // when(tagRepository.findByTagName(anyString())).thenReturn(Optional.empty());
        // when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // when(summaryArchiveRepository.save(any(SummaryArchive.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // openAIClient.chat()은 Mono<String>을 반환하므로, Mono.just()로 감싸서 반환
        when(openAIClient.chat(contains("요약을 만들어주세요"))).thenReturn(Mono.just("Test summary text."));
        when(openAIClient.chat(contains("키워드 3개"))).thenReturn(Mono.just("tag1, tag2, tag3"));


        // Mocking static Files.readString
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.readString(any(Path.class))).thenReturn("This is a long transcript text...");

            // when
            SummaryResponseDTO response = summaryService.summarize(testRequest, 1L);

            // then
            assertNotNull(response);
            assertEquals("Test summary text.", response.getSummary());
            assertEquals(List.of("tag1", "tag2", "tag3"), response.getTags());

            // ArgumentCaptor를 사용하여 save 메서드에 전달된 실제 객체를 캡처
            ArgumentCaptor<Summary> summaryCaptor = ArgumentCaptor.forClass(Summary.class);
            verify(summaryRepository, times(1)).save(summaryCaptor.capture());
            Summary capturedSummary = summaryCaptor.getValue();
            assertEquals("Test summary text.", capturedSummary.getSummaryText());
            assertEquals(testUser, capturedSummary.getUser());

            ArgumentCaptor<SummaryArchive> archiveCaptor = ArgumentCaptor.forClass(SummaryArchive.class);
            verify(summaryArchiveRepository, times(1)).save(archiveCaptor.capture());
            assertEquals(testUser, archiveCaptor.getValue().getUser());

            ArgumentCaptor<UserActivityLog> logCaptor = ArgumentCaptor.forClass(UserActivityLog.class);
            verify(userActivityLogRepository, times(1)).save(logCaptor.capture());
            assertEquals(testUser, logCaptor.getValue().getUser());
        }
    }
} 