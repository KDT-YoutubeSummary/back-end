package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.SummaryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import com.kdt.yts.YouSumback.service.client.OpenAIClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
 * ===============================
 * S3 관련 테스트 import - 주석처리됨
 * DB 직접 사용 테스트로 변경
 * ===============================
 */
/*
// ⭐️⭐️⭐️ S3 관련 Mocking을 위해 필요한 import 추가 ⭐️⭐️⭐️
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
*/

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryServiceImplTest {

    @Mock private OpenAIClient openAIClient;
    @Mock private AnswerOptionRepository answerOptionRepository;
    @Mock private TagRepository tagRepository;
    @Mock private SummaryArchiveRepository summaryArchiveRepository;
    @Mock private SummaryArchiveTagRepository summaryArchiveTagRepository;
    @Mock private UserRepository userRepository;
    @Mock private AudioTranscriptRepository audioTranscriptRepository;
    @Mock private SummaryRepository summaryRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private UserActivityLogRepository userActivityLogRepository;
    
    /*
     * S3Client Mock 주석처리 - DB 직접 사용으로 변경
     */
    /*
    // ⭐️⭐️⭐️ [수정 1] S3Client에 대한 Mock 객체 추가 ⭐️⭐️⭐️
    @Mock private S3Client s3Client;
    */

    @InjectMocks
    private SummaryServiceImpl summaryService;

    private User testUser;
    private Video testVideo;
    private AudioTranscript testTranscript;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userName("testuser")
                .email("test@example.com")
                .build();

        testVideo = Video.builder()
                .id(1L)
                .youtubeId("testVideoId")
                .title("Test Video")
                .originalUrl("https://www.youtube.com/watch?v=testVideoId")
                .uploaderName("Test Channel")
                .thumbnailUrl("https://example.com/thumbnail.jpg")
                .viewCount(1000L)
                .build();

        // transcriptPath는 이제 S3의 객체 키(key)를 의미합니다.
        testTranscript = AudioTranscript.builder()
                .id(1L)
                .video(testVideo)
                .youtubeId("testVideoId")
                .transcriptPath("src/main/resources/textfiles/cleaned_testVideoId.txt") // 로컬 파일 경로
                .transcriptText("Test transcript text stored in DB") // DB에 백업으로 저장된 텍스트
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testSummarize_Success() {
        // Given
        SummaryRequestDTO request = new SummaryRequestDTO();
        request.setOriginalUrl("https://www.youtube.com/watch?v=testVideoId");
        request.setUserPrompt("Test prompt");
        request.setSummaryType(SummaryType.BASIC);

        /*
         * ===============================
         * S3 관련 Mock 설정 - 주석처리됨
         * DB에서 직접 텍스트 읽기로 변경
         * ===============================
         */
        /*
        // ⭐️⭐️⭐️ [수정 2] S3 클라이언트의 동작을 Mocking ⭐️⭐️⭐️
        String mockTranscriptText = "This is a mock transcript text from S3.";
        // S3 응답 객체를 Mocking합니다.
        ResponseBytes<GetObjectResponse> mockResponseBytes = mock(ResponseBytes.class);
        // S3에서 읽어온 byte 배열이 어떤 값을 반환할지 설정합니다.
        when(mockResponseBytes.asByteArray()).thenReturn(mockTranscriptText.getBytes());
        // s3Client의 getObjectAsBytes 메소드가 호출되면, 위에서 만든 Mock 객체를 반환하도록 설정합니다.
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(mockResponseBytes);
        */

        // Repository Mock 설정
        when(audioTranscriptRepository.findByVideo_OriginalUrl(anyString())).thenReturn(Optional.of(testTranscript));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(openAIClient.chat(anyString())).thenReturn(reactor.core.publisher.Mono.just("Mock summary"));
        when(summaryRepository.save(any(Summary.class))).thenReturn(Summary.builder()
                .id(1L)
                .user(testUser)
                .audioTranscript(testTranscript)
                .summaryText("Mock summary")
                .summaryType(SummaryType.BASIC)
                .userPrompt("Test prompt")
                .createdAt(LocalDateTime.now())
                .build());
        when(summaryArchiveRepository.findByUserIdAndSummaryId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        // When
        SummaryResponseDTO result = summaryService.summarize(request, 1L);

        // Then
        assertNotNull(result);
        assertEquals("Mock summary", result.getSummary());
        assertEquals("Test Video", result.getTitle());
        verify(summaryRepository, times(1)).save(any(Summary.class));
    }
}
