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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        UserLibrary userLibrary = UserLibrary.builder()
                .user(user)
                .summary(summary)
                .userNotes(requestDto.getUserNotes())
                .savedAt(LocalDateTime.now())
                .build();

        userLibraryRepository.save(userLibrary);

        // 4. 태그 저장 및 UserLibraryTag 연결
        for (String tagName : requestDto.getTags()) {
            Tag tag = tagRepository.findByTagName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder().tagName(tagName).build()));

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
//                .userNotes(request.getUser_notes())
                .savedAt(LocalDateTime.now())
                .build();

        userLibraryRepository.save(userLibrary);

        // 4. 저장 결과 DTO로 반환
        return UserLibraryResponseDTO.builder()
                .userId(user.getUserId())
                .summaryId(summary.getSummaryId())
//                .userNotes(userLibrary.getUserNotes())
                .build();
    }

    // 특정 유저의 라이브러리 조회
    public List<UserLibraryResponseDTO> getLibrariesByUserId(int userId) {
        // 1. 사용자 존재 여부 확인
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2. 해당 사용자의 라이브러리 조회
        List<UserLibrary> libraries = userLibraryRepository.findByUserUserId(userId);

//        // 3. 각 라이브러리에 대한 태그 조회 및 DTO 변환
//        return libraries.stream().map(library -> {
//            // 라이브러리별 연결된 태그 가져오기
//            List<String> tags = userLibraryTagRepository.findByUserLibrary(library).stream()
//                    .map(userLibraryTag -> userLibraryTag.getTag().getTagName())
//                    .collect(Collectors.toList());
//
//            // DTO 생성 (userNotes 제외, tags 포함)
//            return UserLibraryResponseDTO.builder()
//                    .userLibraryId(library.getUserLibraryId())
//                    .videoTitle(library.getSummary().getAudioTranscript().getVideo().getTitle())
//                    .tags(tags)
//                    .savedAt(library.getSavedAt())
//                    .lastViewedAt(library.getLastViewedAt() != null ? library.getLastViewedAt() : null)
//                    .build();
//        }).collect(Collectors.toList());
        // 3. 각 라이브러리에 대한 태그 조회 및 DTO 변환
        return libraries.stream().map(library -> {
            List<String> tags = userLibraryTagRepository.findByUserLibrary(library).stream()
                    .map(userLibraryTag -> userLibraryTag.getTag().getTagName())
                    .collect(Collectors.toList());

            return UserLibraryResponseDTO.fromEntity(library, tags);
        }).toList();
    }

    // 특정 라이브러리 삭제
    public void deleteLibraryById(Long libraryId) {
        userLibraryRepository.deleteById(libraryId);
    }

    // 제목과 태그로 라이브러리 검색
    public List<UserLibraryResponseDTO> search(String title, String tags) {
        boolean hasTitle = title != null && !title.isBlank();
        boolean hasTags = tags != null && !tags.isBlank();

        // 둘 다 비어있으면 빈 리스트 반환
        if (!hasTitle && !hasTags) {
            return Collections.emptyList();
        }

        List<UserLibrary> result;

        // 제목과 태그가 모두 있는 경우
        if (hasTitle && hasTags) {
            List<UserLibrary> byTitle = userLibraryRepository.findBySummary_SummaryTextContaining(title);
            List<UserLibrary> byTags = userLibraryRepository.findByTagNames(parseTags(tags));
            byTitle.retainAll(byTags);
            result = byTitle;
        } else if (hasTitle) { // 제목만 있는 경우
            result = userLibraryRepository.findBySummary_SummaryTextContaining(title);
        } else { // 태그만 있는 경우
            result = userLibraryRepository.findByTagNames(parseTags(tags));
        }

        // 결과를 DTO로 변환하여 반환
        return result.stream()
                .map(library -> {
                    // 라이브러리별 연결된 태그 가져오기
                    List<String> tagNames = userLibraryTagRepository.findByUserLibrary(library).stream()
                            .map(userLibraryTag -> userLibraryTag.getTag().getTagName())
                            .collect(Collectors.toList());

                    // DTO 생성
                    return UserLibraryResponseDTO.fromEntity(library, tagNames);
                })
                .toList();
    }

    // 태그 문자열을 List<String>으로 변환
    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
