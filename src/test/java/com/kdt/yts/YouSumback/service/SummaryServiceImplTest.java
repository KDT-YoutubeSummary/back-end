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
        String mockTranscriptText = "This is a mock transcript text from S3.";
        ResponseBytes<GetObjectResponse> mockResponseBytes = mock(ResponseBytes.class);
        when(mockResponseBytes.asByteArray()).thenReturn(mockTranscriptText.getBytes(StandardCharsets.UTF_8));
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(mockResponseBytes);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(audioTranscriptRepository.findByVideo_OriginalUrl(anyString())).thenReturn(Optional.of(testTranscript));
        when(summaryArchiveRepository.findByUserIdAndSummaryId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(tagRepository.findByTagName(anyString())).thenReturn(Optional.empty());
        when(summaryArchiveTagRepository.existsById(any())).thenReturn(false);

        // OpenAI 클라이언트 Mocking (요약, 태그 추출 순서대로 다른 값을 반환하도록 설정)
        when(openAIClient.chat(anyString()))
                .thenReturn(Mono.just("Test summary text.")) // 첫 번째 호출(요약)에 대한 응답
                .thenReturn(Mono.just("기술, 인공지능, 코딩")); // 두 번째 호출(태그 추출)에 대한 응답

        // Repository의 save 메서드가 호출될 때 ID가 부여된 객체를 반환하도록 설정
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
            tag.setId(new Random().nextLong(1000L));
            return tag;
        });
        when(userActivityLogRepository.save(any(UserActivityLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(summaryArchiveTagRepository.save(any(SummaryArchiveTag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        SummaryResponseDTO response = summaryService.summarize(testRequest, 1L);

        // then
        // ⭐️⭐️⭐️ [핵심 수정] SummaryResponseDTO의 모든 필드를 검증하도록 로직을 보강합니다. ⭐️⭐️⭐️
        assertNotNull(response, "응답 DTO는 null이 아니어야 합니다.");

        // ID 검증
        assertEquals(100L, response.getSummaryId(), "요약 ID가 일치해야 합니다.");
        assertEquals(1L, response.getTranscriptId(), "대본 ID가 일치해야 합니다.");
        assertEquals(1L, response.getVideoId(), "비디오 ID가 일치해야 합니다.");

        // 내용 검증
        assertEquals("Test summary text.", response.getSummary(), "요약 내용이 일치해야 합니다.");
        assertNotNull(response.getTags(), "태그 목록은 null이 아니어야 합니다.");
        assertEquals(3, response.getTags().size(), "생성된 해시태그는 3개여야 합니다.");
        assertEquals("기술", response.getTags().get(0));

        // 비디오 메타데이터 검증
        assertEquals("test title", response.getTitle(), "영상 제목이 일치해야 합니다.");
        assertEquals("http://thumbnail.url/test.jpg", response.getThumbnailUrl(), "썸네일 URL이 일치해야 합니다.");
        assertEquals("Test Uploader", response.getUploaderName(), "업로더 이름이 일치해야 합니다.");
        assertEquals(1000L, response.getViewCount(), "조회수가 일치해야 합니다.");
        assertEquals("ko", response.getLanguageCode(), "언어 코드가 일치해야 합니다.");
        assertNotNull(response.getCreatedAt(), "생성 시간이 null이 아니어야 합니다.");

        // Repository 메서드 호출 횟수 검증
        verify(summaryRepository, times(1)).save(any(Summary.class));
        verify(summaryArchiveRepository, times(1)).save(any(SummaryArchive.class));
        verify(tagRepository, times(3)).save(any(Tag.class));
        verify(summaryArchiveTagRepository, times(3)).save(any(SummaryArchiveTag.class));
        verify(userActivityLogRepository, times(1)).save(any(UserActivityLog.class));
    }
}