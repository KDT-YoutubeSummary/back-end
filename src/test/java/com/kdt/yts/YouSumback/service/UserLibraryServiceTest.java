package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserLibrarySaveRequestDTO;
import com.kdt.yts.YouSumback.model.entity.Summary;
import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import com.kdt.yts.YouSumback.model.entity.UserLibraryTag;
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

    @Test
    public void testSaveToLibrary_withNewTags_successfullySavesLibraryAndTags() {
        // 1. 테스트용 유저 & 요약 객체 저장
        User testUser = userRepository.save(new User(0, "testuser", "email@test.com", "pw", LocalDateTime.now()));
        Summary testSummary = summaryRepository.save(new Summary(0, "Test Summary", "This is a test summary.", LocalDateTime.now(), testUser));

        // 2. 저장 요청 DTO 생성
        UserLibrarySaveRequestDTO requestDto = new UserLibrarySaveRequestDTO();
        requestDto.setUserId(testUser.getUserId());
        requestDto.setSummaryId(testSummary.getSummaryId());
        requestDto.setTags(List.of("tag1", "tag2", "tag3")); // 새로운 태그 추가
        requestDto.setUserNotes("This is a test note.");

        // 3. 서비스 호출
        userLibraryService.saveToLibrary(requestDto);

        // 4. 결과 검증
        List<UserLibrary> libs = userLibraryRepository.findByUser(testUser);
        assertEquals(1, libs.size());

        UserLibrary userLibrary = libs.get(0);
        List<UserLibraryTag> tags = userLibraryTagRepository.findByUserLibrary(userLibrary);
        
        assertEquals(3, tags.size());

        // 5. 태그 내용 확인
        List<String> tagNames = tags.stream()
                .map(tag -> tag.getTag().getTagName().toString())
                .toList();
        assertTrue(tagNames.containsAll(List.of("tag1", "tag2", "tag3")));
    }
}
