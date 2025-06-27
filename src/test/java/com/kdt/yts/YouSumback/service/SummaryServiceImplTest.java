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
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

// ⭐️⭐️⭐️ S3 관련 Mocking을 위해 필요한 import 추가 ⭐️⭐️⭐️
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    // ⭐️⭐️⭐️ [수정 1] S3Client에 대한 Mock 객체 추가 ⭐️⭐️⭐️
    @Mock private S3Client s3Client;

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

        // transcriptPath는 이제 S3의 객체 키(key)를 의미합니다.
        testTranscript = AudioTranscript.builder()
                .id(1L)
                .video(testVideo)
                .transcriptPath("subtitles/test-id.vtt")
                .build();

        testRequest = new SummaryRequestDTO("http://youtu.be/test-id", "Test Prompt", SummaryType.BASIC);
    }

    @Test
    @DisplayName("요약 및 해시태그 생성 단위 테스트 - 성공 (FR-007, FR-008, FR-009)")
    void summarize_Success() throws IOException {
        // given
        // ⭐️⭐️⭐️ [수정 2] S3 클라이언트의 동작을 Mocking ⭐️⭐️⭐️
        String mockTranscriptText = "This is a mock transcript text from S3.";
        // S3 응답 객체를 Mocking합니다.
        ResponseBytes<GetObjectResponse> mockResponseBytes = mock(ResponseBytes.class);
        // S3에서 읽어온 byte 배열이 어떤 값을 반환할지 설정합니다.
        when(mockResponseBytes.asByteArray()).thenReturn(mockTranscriptText.getBytes(StandardCharsets.UTF_8));
        // s3Client의 getObjectAsBytes 메소드가 호출되면, 위에서 만든 Mock 객체를 반환하도록 설정합니다.
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(mockResponseBytes);


        // --- 기존 Repository Mocking 설정은 그대로 유지 ---
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

        // OpenAI 클라이언트 Mocking은 그대로 유지
        when(openAIClient.chat(anyString())).thenReturn(Mono.just("Test summary text."));


        // when
        // ⭐️⭐️⭐️ [수정 3] 불필요해진 Files, Paths의 static mocking 제거 ⭐️⭐️⭐️
        SummaryResponseDTO response = summaryService.summarize(testRequest, 1L);

        // then
        assertNotNull(response, "응답 DTO는 null이 아니어야 합니다.");
        assertNotNull(response.getSummary(), "요약 내용은 null일 수 없습니다.");
        assertEquals("Test summary text.", response.getSummary());

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
