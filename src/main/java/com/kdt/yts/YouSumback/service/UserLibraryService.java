package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserLibrarySaveRequestDTO;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserLibraryService {

    private final UserRepository userRepository;
    private final SummaryRepository summaryRepository;
    private final TagRepository tagRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final UserLibraryTagRepository userLibraryTagRepository;


    @Transactional
    public void saveToLibrary(UserLibrarySaveRequestDTO requestDto) {
        // 1. 유저 조회
        User user = userRepository.findByUserId(requestDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. 요약 조회
        Summary summary = (Summary) summaryRepository.findBySummaryId(requestDto.getSummaryId())
                .orElseThrow(() -> new IllegalArgumentException("Summary not found"));

        // 3. UserLibrary 저장
        UserLibrary userLibrary = new UserLibrary();
        userLibrary.setUser(user);
        userLibrary.setSummary(summary);
        userLibrary.setSavedAt(LocalDateTime.now());
        userLibrary.setUserNotes(requestDto.getUserNotes());
        userLibraryRepository.save(userLibrary);

        // 4. 태그 저장 및 UserLibraryTag 연결
        for (String tagName : requestDto.getTags()) {
            Tag tag = (Tag) tagRepository.findByTagName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName)));

            // 5. 연결 테이블 저장
            UserLibraryTagId tagId = new UserLibraryTagId(userLibrary.getUserLibraryId(), tag.getTagId());
            UserLibraryTag userLibraryTag = new UserLibraryTag(tagId, userLibrary, tag);
            userLibraryTagRepository.save(userLibraryTag);
        }
    }
}
