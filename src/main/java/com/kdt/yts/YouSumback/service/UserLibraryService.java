package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserLibraryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.UserLibrarySaveRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.UserLibraryResponseDTO;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Getter
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

    public UserLibraryResponseDTO saveLibrary(UserLibraryRequestDTO request) {
        // 1. User 조회 (예외 처리 포함)
        User user = userRepository.findById(request.getUser_id())
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

        // 2. Summary 조회 (예외 처리 포함)
        Summary summary = (Summary) summaryRepository.findBySummaryId(request.getSummary_id())
                .orElseThrow(() -> new IllegalArgumentException("해당 요약이 존재하지 않습니다."));

        // 3. UserLibrary 엔티티 생성 및 저장
        UserLibrary userLibrary = UserLibrary.builder()
                .user(user)
                .summary(summary)
                .userNotes(request.getUser_notes())
                .savedAt(LocalDateTime.now())
                .build();

        userLibraryRepository.save(userLibrary);

        // 4. 저장 결과 DTO로 반환
        return UserLibraryResponseDTO.builder()
                .userId(user.getUserId())
                .summaryId(summary.getSummaryId())
                .userNotes(userLibrary.getUserNotes())
                .build();
    }

    public List<UserLibraryResponseDTO> getLibrariesByUserId(int userId) {
        List<UserLibrary> userLibraries = userLibraryRepository.findByUserUserId(userId);
        return userLibraries.stream()
                .map(UserLibraryResponseDTO::fromEntity)
                .collect(Collectors.toList()
                );
    }

    public void deleteLibraryById(Long libraryId) {
        userLibraryRepository.deleteById(libraryId);
    }

}
