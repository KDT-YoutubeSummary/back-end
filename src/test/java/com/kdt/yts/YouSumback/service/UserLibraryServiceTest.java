package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserLibraryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.UserLibraryResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.UserLibraryResponseListDTO;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserLibraryServiceTest {

    @Autowired private UserLibraryService userLibraryService;
    @Autowired private UserRepository userRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private AudioTranscriptRepository audioTranscriptRepository;
    @Autowired private SummaryRepository summaryRepository;
    @Autowired private UserLibraryRepository userLibraryRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private UserLibraryTagRepository userLibraryTagRepository;

    private User testUser;
    private Summary testSummary;

    @BeforeEach
    void setUp() {
        // 테스트용 유저와 요약 데이터 생성
        testUser = userRepository.save(User.builder()
                .userName("testuser")
                .email("test@example.com")
                .passwordHash("hashed")
                .createdAt(LocalDateTime.now())
                .build());

        // 비디오와 오디오 트랜스크립트 생성
        Video video = videoRepository.save(Video.builder()
                .id(Long.valueOf("vid123"))
                .title("테스트 비디오")
                .originalUrl("http://test.com")
                .uploaderName("Uploader")
                .build());

        AudioTranscript transcript = audioTranscriptRepository.save(AudioTranscript.builder()
                .transcriptText("이건 테스트 스크립트입니다.")
                .createdAt(LocalDateTime.now())
                .video(video)
                .build());

        // 요약 데이터 생성
        testSummary = summaryRepository.save(Summary.builder()
                .summaryText("AI에 관한 요약입니다.")
                .audioTranscript(transcript)
                .user(testUser)
                .languageCode("ko")
                .createdAt(LocalDateTime.now())
                .build());
    }

    // 라이브러리 저장 테스트
//    @Test
//    void saveLibrary_successfullySavesLibraryAndTags() {
//        // given
//        User userId = testUser.getId();
//        UserLibraryRequestDTO dto = new UserLibraryRequestDTO();
//        dto.setSummaryId(Long.valueOf((testSummary.getId())));
//        dto.setUserNotes("좋은 요약이네요!");
//
//        // when
//        userLibraryService.saveLibrary(userId, dto);
//
//        // then
//        List<UserLibrary> libraries = userLibraryRepository.findByUser(testUser);
//        assertEquals(1, libraries.size());
//
//        List<UserLibraryTag> tags = userLibraryTagRepository.findByUserLibrary(libraries.get(0));
//        assertEquals(2, tags.size());
//    }

    // 라이브러리 저장 후 반환 DTO 테스트
    @Test
    void findLibraryByUser_returnsCorrectLibrary() {
        // given
        UserLibrary library = userLibraryRepository.save(UserLibrary.builder()
                .user(testUser)
                .summary(testSummary)
                .userNotes("테스트 메모")
                .savedAt(LocalDateTime.now())
                .lastViewedAt(LocalDateTime.now())
                .build());

        // when
        List<UserLibrary> results = userLibraryRepository.findByUser(testUser);

        // then
        assertEquals(1, results.size());
        assertEquals("테스트 메모", results.get(0).getUserNotes());
    }

    // 라이브러리 삭제 테스트
    @Test
    void deleteLibraryById_removesLibrary() {
        // given
        UserLibrary library = userLibraryRepository.save(UserLibrary.builder()
                .user(testUser)
                .summary(testSummary)
                .savedAt(LocalDateTime.now())
                .build());

        Long id = (long) library.getId();

        // when
        userLibraryService.deleteLibrary(id, testUser.getId());

        // then
        assertFalse(userLibraryRepository.findById(id).isPresent());
    }

    // 라이브러리 검색 테스트
    @Test
    void searchLibraryByTitleAndTags_returnsMatchingResults
            () {
        // given
        UserLibrary library = userLibraryRepository.save(UserLibrary.builder()
                .user(testUser)
                .summary(testSummary)
                .savedAt(LocalDateTime.now())
                .build());

        Tag tag1 = tagRepository.save(Tag.builder().tagName("AI").build());
        Tag tag2 = tagRepository.save(Tag.builder().tagName("추천").build());

        userLibraryTagRepository.save(new UserLibraryTag(
                new UserLibraryTagId(library.getId(), tag1.getId()), library, tag1));
        userLibraryTagRepository.save(new UserLibraryTag(
                new UserLibraryTagId(library.getId(), tag2.getId()), library, tag2));

        // when
        List<UserLibraryResponseListDTO> result = userLibraryService.search(1, "AI", "AI,추천");

        // then
        assertEquals(1, result.size());
        assertEquals(testSummary.getId(), result.get(0).getSummaryId());
    }
}
