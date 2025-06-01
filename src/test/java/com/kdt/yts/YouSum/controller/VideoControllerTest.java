package com.kdt.yts.YouSum.controller;

import com.YouSumback.model.dto.request.VideoRegisterRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void 영상_등록_정상_응답() throws Exception {
        // given
        VideoRegisterRequestDto request = new VideoRegisterRequestDto();
        request.setUsername("Jane Doe");
        request.setTitle("테스트 영상 제목");
        request.setVideoUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ");

        // when & then
        mockMvc.perform(post("/api/src")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.video_url").value("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
    }
}
