package com.kdt.yts.YouSumback.integration;

import com.kdt.yts.YouSumback.config.TestClientConfig;
import com.kdt.yts.YouSumback.model.dto.request.*;
import com.kdt.yts.YouSumback.model.dto.response.ReminderResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.VideoAiRecommendationResponseDTO;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import com.kdt.yts.YouSumback.service.*;
import com.kdt.yts.YouSumback.service.client.OpenAIClient;
import com.kdt.yts.YouSumback.service.client.YouTubeClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@Transactional
@Import(TestClientConfig.class)
@SpringBootTest(properties = {
        "youtube.api-key=dummy-youtube-key",
        "google.oauth.client-id=dummy-client-id",
        "google.oauth.client-secret=dummy-client-secret"
}) // 환경 변수를 설정하여 실제 API 호출을 피합니다.
public class ApplicationFlowIntegrationTest {
    @Autowired
    private AuthService authService;
    @Autowired
    private SummaryService summaryService;
    @Autowired
    private UserLibraryService userLibraryService;
    @Autowired
    private ReminderService reminderService;
    @Autowired
    private VideoRecommendationService videoRecommendationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private AudioTranscriptRepository audioTranscriptRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private UserLibraryTagRepository userLibraryTagRepository;
    @Autowired
    private AudioTranscriptRepository transcriptRepository;

    @Autowired
    // OpenAI 클라이언트는 실제 API 호출을 피하기 위해 모킹
    private com.kdt.yts.YouSumback.service.client.OpenAIClient openAIClient;

    @Autowired
    // YouTube 클라이언트도 실제 API 호출을 피하기 위해 모킹
    private com.kdt.yts.YouSumback.service.client.YouTubeClient youTubeClient;

    private Video video;

    @BeforeEach
    void setUp() throws IOException {
        // ✅ 1. 더미 비디오 저장
        video = videoRepository.save(Video.builder()
                .youtubeId("extended123")
                .title("확장 테스트 비디오")
                .originalUrl("http://extended.test/video")
                .uploaderName("ExtendedUploader")
                .thumbnailUrl("http://extended.test/thumbnail")
                .viewCount(1000L)
                .publishedAt(LocalDateTime.now())
                .originalLanguageCode("ko")
                .durationSeconds(300)
                .build());

        // ✅ 2. 실제 텍스트 파일 생성
        Path transcriptPath = Paths.get("test-transcripts", "extended123.txt");
        Files.createDirectories(transcriptPath.getParent());
        Files.writeString(transcriptPath, "이건 요약 테스트용 텍스트입니다."); // 텍스트 비우지 말기!

        // ✅ 3. AudioTranscript 저장 (절대 경로 저장)
        AudioTranscript transcript = AudioTranscript.builder()
                .video(video)
                .transcriptPath(transcriptPath.toAbsolutePath().toString())
                .createdAt(LocalDateTime.now())
                .youtubeId("extended123")
                .build();
        audioTranscriptRepository.save(transcript);
    }

    @Test
    @DisplayName("애플리케이션 흐름 통합 테스트")
    void testExtendedApplicationFlow() {
        // 1. 회원가입
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setUserName("extendedUser");
        registerRequest.setEmail("extended@test.com");
        registerRequest.setPassword("extended123");

        User registeredUser = authService.register(registerRequest);
        assertNotNull(registeredUser);
        assertEquals("extendedUser", registeredUser.getUserName());

        // 2. 로그인
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUserName("extendedUser");
        loginRequest.setPassword("extended123");

        String token = authService.authenticate(loginRequest);
        assertNotNull(token);

        // 3. 트랜스크립트 조회 (BeforeEach에서 이미 생성됨)
        AudioTranscript transcript = audioTranscriptRepository.findByYoutubeId("extended123")
                .orElseThrow(() -> new RuntimeException("Transcript not found"));

//        // 3. 비디오 및 트랜스크립트 생성
//        Video video = videoRepository.save(Video.builder()
//                .title("확장 테스트 비디오")
//                .originalUrl("http://extended.test/video")
//                .uploaderName("ExtendedUploader")
//                .originalLanguageCode("ko")
//                .durationSeconds(300)
//                .youtubeId("extended123")
//                .publishedAt(LocalDateTime.now())
//                .thumbnailUrl("http://extended.test/thumbnail")
//                .viewCount(1000L)
//                .build());
//
//        AudioTranscript transcript = audioTranscriptRepository.save(AudioTranscript.builder()
//                .transcriptPath("src/main/resources/textfiles/extended123.txt")
//                .youtubeId("extended123")
//                .createdAt(LocalDateTime.now())
//                .video(video)
//                .build());

        // 4. 요약 생성
        SummaryRequestDTO summaryRequest = new SummaryRequestDTO();
        summaryRequest.setUserId(registeredUser.getId());
        summaryRequest.setTranscriptId(transcript.getId());
        summaryRequest.setUserPrompt("확장 테스트용 요약을 생성해주세요");

        SummaryResponseDTO summaryResponse = summaryService.summarize(summaryRequest);
        assertNotNull(summaryResponse);
        assertNotNull(summaryResponse.getSummaryId());

        // 5. 라이브러리에 저장
        UserLibraryRequestDTO libraryRequest = new UserLibraryRequestDTO();
        libraryRequest.setSummaryId(summaryResponse.getSummaryId());
        libraryRequest.setUserNotes("확장 테스트 메모");

        userLibraryService.saveLibrary(registeredUser.getId(), libraryRequest);

        // 6. 리마인더 설정
        ReminderCreateRequestDTO reminderRequest = new ReminderCreateRequestDTO();
        reminderRequest.setUserId(registeredUser.getId());
        reminderRequest.setUserLibraryId(summaryResponse.getSummaryId());
        reminderRequest.setReminderType(ReminderType.DAILY);
        reminderRequest.setFrequencyInterval(1);
        reminderRequest.setBaseDatetimeForRecurrence(LocalDateTime.now().plusHours(1));
        reminderRequest.setReminderNote("매일 확인할 내용");

        ReminderResponseDTO reminderResponse = reminderService.createReminder(reminderRequest);
        assertNotNull(reminderResponse);
        assertNotNull(reminderResponse.getReminderId());

        // 7. AI 추천 영상 요청
        List<VideoAiRecommendationResponseDTO> recommendations =
                videoRecommendationService.getAiRecommendationByUserLibraryId(summaryResponse.getSummaryId()).block();

        // 추천 결과 검증
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        assertEquals(2, recommendations.size());
        assertEquals("추천 비디오 1", recommendations.get(0).getTitle());
        assertEquals("http://recommend.test/video1", recommendations.get(0).getUrl());

        // 8. 추천 결과 저장
        List<VideoRecommendation> savedRecommendations =
                videoRecommendationService.saveAiRecommendation(summaryResponse.getSummaryId(), recommendations);

        // 저장된 추천 검증
        assertNotNull(savedRecommendations);
        assertEquals(2, savedRecommendations.size());
        assertEquals(registeredUser.getId(), savedRecommendations.get(0).getUser().getId());
        assertEquals(video.getId(), savedRecommendations.get(0).getSourceVideo().getId());
        assertNotNull(savedRecommendations.get(0).getRecommendationReason());

        // 9. 사용자 ID로 추천 목록 조회
        List<VideoRecommendation> userRecommendations =
                videoRecommendationService.getRecommendationsByUserId(registeredUser.getId());

        // 조회된 추천 목록 검증
        assertNotNull(userRecommendations);
        assertFalse(userRecommendations.isEmpty());
        assertTrue(userRecommendations.stream()
                .anyMatch(r -> r.getId().equals(savedRecommendations.get(0).getId())));
    }
}