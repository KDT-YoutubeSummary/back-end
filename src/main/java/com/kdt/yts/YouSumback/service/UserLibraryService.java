package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.UserLibraryRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.UserNoteUpdateRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.TagStatResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.UserLibraryResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.UserLibraryResponseListDTO;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
//@Getter
@Transactional
// UserLibraryService는 유저의 라이브러리 관련 기능을 처리하는 서비스입니다.
public class UserLibraryService {

    private final UserRepository userRepository;
    private final SummaryRepository summaryRepository;
    private final UserLibraryRepository userLibraryRepository;
    private final UserLibraryTagRepository userLibraryTagRepository;
    private final TagRepository tagRepository;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 유저 라이브러리 저장
    @Transactional
    public UserLibraryResponseListDTO saveLibrary(Long userId, UserLibraryRequestDTO request) {
        // 1. User 조회 (예외 처리 포함)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));
// User는 현재 JWT로 인증된 상태이므로, request에서 userId를 가져와서 처리

        // 2. Summary 조회 (예외 처리 포함)
        Summary summary = summaryRepository.findById(request.getSummaryId())
                .orElseThrow(() -> new IllegalArgumentException("해당 요약이 존재하지 않습니다."));

        // 3. UserLibrary 엔티티 생성 및 저장
        UserLibrary userLibrary = UserLibrary.builder()
                .user(user)
                .summary(summary)
                .userNotes(request.getUserNotes())
                .savedAt(LocalDateTime.now())
                .lastViewedAt(LocalDateTime.now())
                .build();

        userLibraryRepository.save(userLibrary);


        // 4. 태그 저장 및 매핑
        if (request.getTags() != null) {
            for (String tagName : request.getTags()) {
                Tag tag = tagRepository.findByTagName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().tagName(tagName).build()));

                //  중복 저장 방지
                boolean exists = userLibraryTagRepository.existsByUserLibraryAndTag(userLibrary, tag);
                if (!exists) {
                    UserLibraryTagId tagId = new UserLibraryTagId(userLibrary.getId(), tag.getId());
                    UserLibraryTag userLibraryTag = new UserLibraryTag(tagId, userLibrary, tag);
                    userLibraryTagRepository.save(userLibraryTag);
                }
            }
        }

        // 태그 이름 리스트
        List<String> tagNames = userLibraryTagRepository.findByUserLibrary(userLibrary).stream()
                .map(t -> t.getTag().getTagName()).toList();

        return UserLibraryResponseListDTO.fromEntity(userLibrary, tagNames);
    }

    // 특정 유저의 라이브러리 목록 전체 조회
    @Transactional
    public List<UserLibraryResponseListDTO> getLibrariesByUserId(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        List<UserLibrary> libraries = userLibraryRepository.findByUserId(userId);

        return libraries.stream()
                .map(library -> {
                    List<UserLibraryTag> tags = userLibraryTagRepository.findByUserLibrary(library);
                    List<String> tagNames = tags.stream()
                            .map(t -> t.getTag().getTagName())
                            .toList();

                    return UserLibraryResponseListDTO.fromEntity(library, tagNames);
                })
                .toList();
    }

    // 특정 라이브러리 상세 조회 (요약 본문 포함)
    public UserLibraryResponseDTO getLibraryDetail(Long libraryId) {
        UserLibrary library = userLibraryRepository.findById(libraryId)
                .orElseThrow(() -> new NoSuchElementException("해당 라이브러리를 찾을 수 없습니다."));

        List<UserLibraryTag> tags = userLibraryTagRepository.findByUserLibrary(library);
        List<String> tagNames = tags.stream()
                .map(t -> t.getTag().getTagName())
                .toList();

        return UserLibraryResponseDTO.fromEntity(library, tagNames);
    }

    // 특정 라이브러리 삭제
    @Transactional
    public void deleteLibrary(Long libraryId) {
        UserLibrary library = userLibraryRepository.findById(libraryId)
                .orElseThrow(() -> new NoSuchElementException("해당 라이브러리를 찾을 수 없습니다."));

        // 관련 태그 연결 먼저 삭제
        userLibraryTagRepository.deleteAllByUserLibrary(library);

        // 2라이브러리 삭제
        userLibraryRepository.delete(library);
    }

    // 제목과 태그로 라이브러리 검색
    public List<UserLibraryResponseListDTO> search(long userId, String title, String tags) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 해당 사용자의 라이브러리 조회 (필요시 사용)
        List<UserLibrary> userLibraries = userLibraryRepository.findByUser(user);

        return userLibraries.stream().filter(userLibrary ->  {
            boolean titleMatches = (title == null ||
                    userLibrary.getSummary().getTranscript().getVideo().getTitle().contains(title));

                    boolean tagMatches = true;
                    if (tags != null) {
                        List<String> filterTags = Arrays.asList(tags.split(","));
                        List<String> actualTags = userLibraryTagRepository.findByUserLibrary(userLibrary).stream()
                                .map(t -> t.getTag().getTagName()).collect(Collectors.toList());
                        tagMatches = filterTags.stream().allMatch(actualTags::contains);
                    }
                    return titleMatches && tagMatches;
        }).map(library -> {
            List<String> tagList = userLibraryTagRepository.findByUserLibrary(library).stream()
                    .map(userLibraryTag -> userLibraryTag.getTag().getTagName())
                    .toList();

                    return UserLibraryResponseListDTO.fromEntity(library, tagList);
                })
                .collect(Collectors.toList());
    }

    // 유저별 태그 통계 조회
    public List<TagStatResponseDTO> getTagStatsByUser(Long userId) {
        List<Object[]> result = userLibraryRepository.countTagsByUser_UserId(userId);

        return result.stream()
                .map(row -> new TagStatResponseDTO((String) row[0], (Long) row[1]))
                .toList();
    }

    // 유저 라이브러리 메모 업데이트
    @Transactional
    public void updateUserNotes(Long userId,UserNoteUpdateRequestDTO requestDTO) {
        UserLibrary library = userLibraryRepository.findById(requestDTO.getUserLibraryId())
                .orElseThrow(() -> new IllegalArgumentException("해당 라이브러리를 찾을 수 없습니다."));

        if (!library.getUser().getId().equals(userId)) {
            throw new SecurityException("해당 라이브러리에 대한 권한이 없습니다.");
        }

        library.setUserNotes(requestDTO.getUserNotes());
    }

}
