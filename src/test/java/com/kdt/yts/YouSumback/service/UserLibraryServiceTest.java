package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserLibrarySaveRequestDTO;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase
public class UserLibraryServiceTest {

    @Autowired
    private UserLibraryService userLibraryService;

    @Autowired
    private UserLibraryRepository userLibraryRepository;

    @Autowired
    private SummaryRepository summaryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserLibraryTagRepository userLibraryTagRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AudioTranscriptRepository audioTranscriptRepository;

    // 새로운 태그를 추가하여 라이브러리에 저장하는 테스트
    @Test
    public void testSaveToLibrary_withNewTags_successfullySavesLibraryAndTags() {
        // 1. 테스트용 유저 저장
        User testUser = userRepository.save(
                User.builder()
                        .userName("testuser")
                        .email("email@test.com")
                        .passwordHash("pw")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        // 2. 테스트용 음성 텍스트 저장
        AudioTranscript dummyTranscript = new AudioTranscript();
        dummyTranscript.setTranscriptText("This is a dummy transcript.");
        dummyTranscript = audioTranscriptRepository.save(dummyTranscript);

        // 3. 테스트용 요약 저장
        Summary testSummary = summaryRepository.save(
                Summary.builder()
                        .summaryText("This is a test summary.")
                        .audioTranscript(dummyTranscript)
                        .user(testUser)
                        .languageCode("ko")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // 4. 저장 요청 DTO 생성
        UserLibrarySaveRequestDTO requestDto = new UserLibrarySaveRequestDTO();
        requestDto.setUserId(testUser.getUserId());
        requestDto.setSummaryId(testSummary.getSummaryId());
        requestDto.setTags(List.of("tag1", "tag2", "tag3")); // 새로운 태그 추가
        requestDto.setUserNotes("This is a test note.");

        // 5. 서비스 호출
        userLibraryService.saveToLibrary(requestDto);

        // 6. 결과 검증
        List<UserLibrary> libs = userLibraryRepository.findByUser(testUser);
        assertEquals(1, libs.size());

        UserLibrary userLibrary = libs.get(0);
        List<UserLibraryTag> tags = userLibraryTagRepository.findByUserLibrary(userLibrary);
        assertEquals(3, tags.size());

        // 7. 태그 내용 확인
        List<String> tagNames = tags.stream()
                .map(tag -> tag.getTag().getTagName())
                .toList();
        assertTrue(tagNames.containsAll(List.of("tag1", "tag2", "tag3")));

    }
}