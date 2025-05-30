package com.kdt.yts.YouSumback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UserLibraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SummaryRepository summaryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AudioTranscriptRepository audioTranscriptRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    UserLibraryRepository userLibraryRepository;

    private Integer savedUserId;
    private Integer savedSummaryId;
    private Long savedLibraryId;

    // 테스트 실행 전: 테스트용 User와 Summary 데이터를 DB에 저장
    @BeforeEach
    public void setUp() throws Exception {
        // 1. 테스트용 유저 저장
        User testUser = userRepository.save(
                User.builder()
                        .userName("testuser")
                        .email("email@test.com")
                        .passwordHash("pw")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        //  2. 테스트용 비디오 저장
        Video testVideo = videoRepository.save(
                Video.builder()
                        .videoId("test-video-id")
                        .title("Test Video")
                        .originalUrl("http://test.video")
                        .uploaderName("Uploader")
                        .originalLanguageCode("ko")
                        .durationSeconds(300)
                        .build()
        );

        //  3. AudioTranscript에 video 설정 포함
        AudioTranscript dummyTranscript = audioTranscriptRepository.save(
                AudioTranscript.builder()
                        .transcriptText("이건 테스트 스크립트입니다.")
                        .createdAt(LocalDateTime.now())
                        .video(testVideo) // 비디오 설정
                        .build()
        );
        dummyTranscript = audioTranscriptRepository.save(dummyTranscript);

        //  4. Summary 저장
        Summary summary = summaryRepository.save(Summary.builder()
                .summaryText("테스트 요약")
                .audioTranscript(dummyTranscript)
                .user(testUser)
                .languageCode("ko")
                .createdAt(LocalDateTime.now())
                .build());

        savedUserId = testUser.getUserId();
        savedSummaryId = summary.getSummaryId();

        //  5. UserLibrary 저장
        UserLibrary savedLibrary = userLibraryRepository.save(UserLibrary.builder()
                .user(testUser)
                .summary(summary)
                .userNotes("테스트 메모")
                .savedAt(LocalDateTime.now())
                .lastViewedAt(LocalDateTime.now())
                .build());

        savedLibraryId = savedLibrary.getUserLibraryId();
    }

    @Test
    // 라이브러리 저장 API 테스트
    public void saveLibrary_ShouldReturn201Created() throws Exception {
        // GIVEN: 라이브러리 저장 요청 DTO와 동일한 구조의 JSON 요청 본문 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("user_id", savedUserId);
        requestBody.put("summary_id", savedSummaryId);
        requestBody.put("user_notes", "사용자 메모");

        // WHEN & THEN: POST 요청 수행 → 응답 상태/구조/값 검증
        mockMvc.perform(post("/api/library")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("라이브러리 등록 완료"))
                .andExpect(jsonPath("$.data.user_id").value(savedUserId))
                .andExpect(jsonPath("$.data.summary_id").value(savedSummaryId))
                .andExpect(jsonPath("$.data.user_notes").value("사용자 메모"));
    }

    // 라이브러리 조회 API 테스트
    @Test
    public void getLibrariesByUserId_ShouldReturnLibraryList() throws Exception {
        mockMvc.perform(get("/api/library")
                        .param("user_id", savedUserId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("라이브러리 조회 성공"))
                .andExpect(jsonPath("$.data").isArray());
    }

    // 라이브러리 삭제 API 테스트
    @Test
    public void deleteLibraryById_ShouldReturn204NoContent() throws Exception {
        mockMvc.perform(delete("/api/library/{libraryId}", savedLibraryId))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    // 라이브러리 검색 API 테스트
    // 1. 제목으로 검색
    @Test
    @DisplayName("Search by title only")
    void searchByTitleOnly() throws Exception {
        mockMvc.perform(get("/api/library/search")
                        .param("title", "AI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // 2. 태그로 검색
    @Test
    @DisplayName("Search by tags only")
    void searchByTagsOnly() throws Exception {
        mockMvc.perform(get("/api/library/search")
                        .param("tags", "추천,학습"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // 3. 제목과 태그로 검색
    @Test
    @DisplayName("Search by title and tags")
    void searchByTitleAndTags() throws Exception {
        mockMvc.perform(get("/api/library/search")
                        .param("title", "AI")
                        .param("tags", "추천,학습"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // 4. 검색어가 없는 경우
    @Test
    @DisplayName("Search with no parameters")
    void searchWithNoParams() throws Exception {
        mockMvc.perform(get("/api/library/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}