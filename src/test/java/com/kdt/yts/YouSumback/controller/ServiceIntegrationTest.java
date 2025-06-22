package com.kdt.yts.YouSumback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.yts.YouSumback.model.dto.request.SummaryArchiveRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.LoginResponseDTO;
import com.kdt.yts.YouSumback.model.entity.ReminderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.Disabled;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 라이브러리, 퀴즈, 리마인더 등 주요 서비스 기능에 대한 통합 테스트 클래스.
 * 이를 통해 일관된 환경에서 서비스 기능의 연동 동작을 검증합니다.
 */
@Disabled("Disabling integration tests to focus on DB configuration issues.")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;

    /**
     * 각 테스트 실행 전에 실행되는 설정 메서드.
     * data.sql에 미리 정의된 테스트 사용자('test10@example.com')로 로그인하여
     * 후속 API 호출에 필요한 JWT 인증 토큰을 획득하고 'jwtToken' 필드에 저장합니다.
     */
    @BeforeEach
    void setUp() throws Exception {
        // given: 로그인 정보
        var loginRequest = new Object() {
            public final String userName = "test10@example.com"; // In Spring Security, username is email
            public final String password = "password";
        };

        // when: 로그인 요청
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // then: 토큰 발급
        String responseBody = result.getResponse().getContentAsString();
        LoginResponseDTO loginResponse = objectMapper.readValue(responseBody, LoginResponseDTO.class);
        this.jwtToken = loginResponse.getAccessToken();
    }

    /**
     * FR-011 (라이브러리 저장) 요구사항을 검증합니다.
     * 인증된 사용자가 data.sql에 정의된 요약(summaryId=1)을
     * 자신의 라이브러리에 성공적으로 저장하는지 확인합니다.
     */
    @Test
    @DisplayName("라이브러리 저장 테스트 - 성공 (FR-011)")
    void givenSummary_whenSaveToArchive_thenSucceed() throws Exception {
        // given: 저장할 요약 정보 (summaryId=1 from data.sql)
        SummaryArchiveRequestDTO request = new SummaryArchiveRequestDTO();
        request.setSummaryId(1L);
        request.setUserNotes("My custom note");
        request.setTags(List.of("new-tag1", "new-tag2"));

        // when & then: 라이브러리에 저장 요청
        mockMvc.perform(post("/api/summary-archives")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("요약 저장소 등록 완료"))
                .andDo(print());
    }

    /**
     * FR-016 (문제 자동 생성) 요구사항을 검증합니다.
     * data.sql의 요약(summaryId=1)을 기반으로 퀴즈 생성을 요청했을 때,
     * 외부 AI API 연동(실제 또는 Mock)을 통해 퀴즈 정보가 정상적으로 반환되는지 확인합니다.
     * 참고: 이 테스트는 실제 OpenAI API를 호출할 수 있으므로, 외부 환경에 따라 결과가 달라질 수 있습니다.
     */
    @Test
    @DisplayName("퀴즈 생성 테스트 - 성공 (FR-016)")
    void givenSummary_whenGenerateQuiz_thenSucceed() throws Exception {
        // given: 퀴즈 생성 요청 정보 (summaryId=1 from data.sql)
        var quizRequest = new Object() {
            public final Long summaryId = 1L;
            public final Integer questionCount = 3;
        };

        // when & then: 퀴즈 생성 요청
        // Note: This test might fail if it relies on an external API (OpenAI)
        // It's better to mock the service layer for this kind of test.
        // For now, we'll proceed assuming it can be tested in an integration environment.
        mockMvc.perform(post("/api/quizzes/generate")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andDo(print());
    }

    /**
     * FR-012 (라이브러리 검색) 요구사항을 제목 기반으로 검증합니다.
     * data.sql에 저장된 'AI Summary Test'라는 제목으로 검색했을 때,
     * 해당 요약 저장소 정보를 정확히 찾아내는지 확인합니다.
     */
    @Test
    @DisplayName("라이브러리 검색 테스트 (제목) - 성공 (FR-012)")
    void givenTitleQuery_whenSearchArchive_thenSucceed() throws Exception {
        // when & then
        mockMvc.perform(get("/api/summary-archives/search")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("title", "AI Summary Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("AI Summary Test"))
                .andDo(print());
    }

    /**
     * FR-012 (라이브러리 검색) 요구사항을 태그 기반으로 검증합니다.
     * data.sql에 저장된 'AI-Test' 태그로 검색했을 때,
     * 해당 태그를 가진 요약 저장소 정보를 정확히 찾아내는지 확인합니다.
     */
    @Test
    @DisplayName("라이브러리 검색 테스트 (태그) - 성공 (FR-012)")
    void givenTagQuery_whenSearchArchive_thenSucceed() throws Exception {
        // when & then
        mockMvc.perform(get("/api/summary-archives/search")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("tags", "AI-Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tags[0]").value("AI-Test"))
                .andDo(print());
    }

    /**
     * FR-013 (태그 통계 시각화) 요구사항을 검증합니다.
     * 태그 통계 API를 호출했을 때, 사용자가 저장한 태그 목록과 각 태그별 개수가
     * 올바른 형식으로 반환되는지 확인합니다.
     */
    @Test
    @DisplayName("태그 통계 조회 테스트 - 성공 (FR-013)")
    void whenGetTagStatistics_thenSucceed() throws Exception {
        // when & then
        mockMvc.perform(get("/api/summary-archives/stat/tags")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].tagName").exists())
                .andExpect(jsonPath("$.data[0].count").exists())
                .andDo(print());
    }

    /**
     * FR-017 (문제 정답/해설 제공) 요구사항을 검증합니다.
     * data.sql에 생성된 퀴즈(quizId=10)에 대해 정답과 오답을 섞어 제출하고,
     * 서버가 정답 수를 정확히 계산하여 점수와 함께 반환하는지 확인합니다.
     */
    @Test
    @DisplayName("퀴즈 제출 테스트 - 성공 (FR-017)")
    void givenQuizAnswers_whenSubmitQuiz_thenSucceed() throws Exception {
        // given: 퀴즈 ID 10에 대한 정답 제출
        var answer1 = new Object() {
            public final Long questionId = 10L;
            public final Long answerOptionId = 10L; // Correct answer
        };
        var answer2 = new Object() {
            public final Long questionId = 11L;
            public final Long answerOptionId = 13L; // Incorrect answer
        };
        var quizSubmission = new Object() {
            public final List<Object> answers = List.of(answer1, answer2);
        };

        // when & then
        mockMvc.perform(post("/api/quizzes/10/submit")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizSubmission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizId").value(10))
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.correctAnswers").value(1))
                .andExpect(jsonPath("$.score").value(50.0))
                .andDo(print());
    }

    /**
     * FR-014 (리마인드 알림) 요구사항을 검증합니다.
     * 특정 요약 저장소에 대한 리마인더 생성을 요청하고,
     * 성공적으로 생성되어 ID가 부여되는지 확인합니다.
     */
    @Test
    @DisplayName("리마인더 생성 테스트 - 성공 (FR-014)")
    void givenReminderInfo_whenCreateReminder_thenSucceed() throws Exception {
        // given
        var reminderRequest = new Object() {
            public final Long userId = 10L;
            public final Long summaryArchiveId = 10L;
            public final ReminderType reminderType = ReminderType.DAILY;
            public final String reminderMessage = "Review this summary!";
            public final LocalDateTime reminderTime = LocalDateTime.now().plusDays(1);
        };

        // when & then
        mockMvc.perform(post("/api/reminders")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reminderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reminderId").exists())
                .andDo(print());
    }

    /**
     * FR-015 (학습 영상 추천) 요구사항을 검증합니다.
     * 사용자 ID를 기반으로 추천 영상 목록을 조회하고,
     * data.sql에 정의된 추천 데이터가 정상적으로 반환되는지 확인합니다.
     */
    @Test
    @DisplayName("학습 영상 추천 조회 테스트 - 성공 (FR-015)")
    void whenGetRecommendations_thenSucceed() throws Exception {
        // when & then
        mockMvc.perform(get("/api/recommendations/users/10")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].reason").value("Because you are testing"))
                .andDo(print());
    }
} 