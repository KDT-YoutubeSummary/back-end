package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.SummaryArchiveRequestDTO;
import com.kdt.yts.YouSumback.model.dto.request.UserNoteUpdateRequestDTO;
import com.kdt.yts.YouSumback.model.dto.response.TagStatResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryArchiveResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.SummaryArchiveResponseListDTO;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
// SummaryArchiveService는 유저의 요약 저장소 관련 기능을 처리하는 서비스입니다.
public class SummaryArchiveService {

    private final UserRepository userRepository;
    private final SummaryRepository summaryRepository;
    private final SummaryArchiveRepository summaryArchiveRepository;
    private final SummaryArchiveTagRepository summaryArchiveTagRepository;
    private final TagRepository tagRepository;
    private final UserActivityLogRepository userActivityLogRepository;

    // 요약 저장소 저장
    @Transactional
    public SummaryArchiveResponseListDTO saveArchive(Long userId, SummaryArchiveRequestDTO request) {
        // 1. User 조회 (예외 처리 포함)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));

        // 2. Summary 조회 (예외 처리 포함)
        Summary summary = summaryRepository.findById(request.getSummaryId())
                .orElseThrow(() -> new IllegalArgumentException("해당 요약이 존재하지 않습니다."));

        // 3. 중복 저장 방지
        Optional<SummaryArchive> existingArchive = summaryArchiveRepository.findByUserIdAndSummaryId(userId, request.getSummaryId());
        if (existingArchive.isPresent()) {
            throw new IllegalStateException("이미 저장된 요약입니다.");
        }

        // 4. SummaryArchive 엔티티 생성 및 저장
        SummaryArchive summaryArchive = new SummaryArchive();
        summaryArchive.setUser(user);
        summaryArchive.setSummary(summary);
        summaryArchive.setUserNotes(request.getUserNotes());
        summaryArchive.setLastViewedAt(LocalDateTime.now());

        summaryArchiveRepository.save(summaryArchive);

        // 5. 태그 저장 및 매핑
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tagName : request.getTags()) {
                if (tagName != null && !tagName.trim().isEmpty()) {
                    Tag tag = findOrCreateTag(tagName.trim());
                    SummaryArchiveTag summaryArchiveTag = new SummaryArchiveTag(summaryArchive.getId(), tag.getId());
                    summaryArchiveTagRepository.save(summaryArchiveTag);
                }
            }
        }

        // 태그 이름 리스트
        List<String> tagNames = summaryArchiveTagRepository.findBySummaryArchiveId(summaryArchive.getId()).stream()
                .map(t -> t.getTag().getTagName()).toList();

        return SummaryArchiveResponseListDTO.fromEntity(summaryArchive, tagNames);
    }

    // 특정 유저의 요약 저장소 목록 전체 조회
    @Transactional(readOnly = true)
    public List<SummaryArchiveResponseListDTO> getArchivesByUserId(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        List<SummaryArchive> archives = summaryArchiveRepository.findByUserId(userId);

        return archives.stream()
                .map(archive -> {
                    List<SummaryArchiveTag> tags = summaryArchiveTagRepository.findBySummaryArchiveId(archive.getId());
                    List<String> tagNames = tags.stream()
                            .map(t -> t.getTag().getTagName())
                            .toList();

                    return SummaryArchiveResponseListDTO.fromEntity(archive, tagNames);
                })
                .toList();
    }

    // 특정 요약 저장소 상세 조회 (요약 본문 포함)
    @Transactional(readOnly = true)
    public SummaryArchiveResponseDTO getArchiveDetail(Long archiveId, Long userId) {
        SummaryArchive archive = summaryArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new NoSuchElementException("해당 요약 저장소를 찾을 수 없습니다."));

        if (!archive.getUser().getId().equals(userId)) {
            throw new SecurityException("해당 요약 저장소에 접근 권한이 없습니다.");
        }

        // 마지막 조회 시간 업데이트
        archive.setLastViewedAt(LocalDateTime.now());
        summaryArchiveRepository.save(archive);

        List<SummaryArchiveTag> tags = summaryArchiveTagRepository.findBySummaryArchiveId(archive.getId());
        List<String> tagNames = tags.stream().map(t -> t.getTag().getTagName()).toList();

        return SummaryArchiveResponseDTO.fromEntity(archive, tagNames);
    }

    // 특정 요약 저장소 삭제
    @Transactional
    public void deleteArchive(Long archiveId, Long userId) {
        SummaryArchive archive = summaryArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new NoSuchElementException("해당 요약 저장소를 찾을 수 없습니다."));

        if (!archive.getUser().getId().equals(userId)) {
            throw new SecurityException("해당 요약 저장소에 대한 권한이 없습니다.");
        }

        summaryArchiveRepository.delete(archive);
    }

    // 제목과 태그로 요약 저장소 검색
    @Transactional(readOnly = true)
    public List<SummaryArchiveResponseListDTO> search(long userId, String title, String tags) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        List<SummaryArchive> summaryArchives = summaryArchiveRepository.findByUserId(userId);

        return summaryArchives.stream().filter(summaryArchive -> {
            boolean titleMatches = (title == null ||
                    summaryArchive.getSummary().getAudioTranscript().getVideo().getTitle().contains(title));

            boolean tagMatches = true;
            if (tags != null) {
                List<String> filterTags = Arrays.stream(tags.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();
                List<String> actualTags = summaryArchiveTagRepository.findBySummaryArchiveId(summaryArchive.getId()).stream()
                        .map(t -> t.getTag().getTagName()).toList();
                tagMatches = new HashSet<>(actualTags).containsAll(filterTags);
            }
            return titleMatches && tagMatches;
        }).map(archive -> {
            List<String> tagList = summaryArchiveTagRepository.findBySummaryArchiveId(archive.getId()).stream()
                    .map(summaryArchiveTag -> summaryArchiveTag.getTag().getTagName())
                    .toList();

            return SummaryArchiveResponseListDTO.fromEntity(archive, tagList);
        })
        .collect(Collectors.toList());
    }

    // 유저별 태그 통계 조회
    @Transactional(readOnly = true)
    public List<TagStatResponseDTO> getTagStatsByUser(Long userId) {
        List<Object[]> result = summaryArchiveTagRepository.countTagsByUserId(userId);

        return result.stream()
                .map(row -> new TagStatResponseDTO(
                        String.valueOf(row[0]),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    // 요약 저장소 메모 업데이트
    @Transactional
    public void updateUserNotes(Long userId, UserNoteUpdateRequestDTO requestDTO) {
        log.info("Starting memo update for userId: {}, summaryArchiveId: {}", userId, requestDTO.getSummaryArchiveId());
        
        try {
            SummaryArchive archive = summaryArchiveRepository.findById(requestDTO.getSummaryArchiveId())
                    .orElseThrow(() -> new IllegalArgumentException("요약 저장소를 찾을 수 없습니다."));

            log.info("Found archive with ID: {}, current owner: {}", archive.getId(), archive.getUser().getId());

            if (!archive.getUser().getId().equals(userId)) {
                log.warn("Access denied: User {} tried to modify archive owned by {}", userId, archive.getUser().getId());
                throw new SecurityException("해당 요약 저장소에 대한 권한이 없습니다.");
            }
            
            String oldNote = archive.getUserNotes();
            archive.setUserNotes(requestDTO.getNote());
            log.info("Updated note: '{}' -> '{}'", oldNote, requestDTO.getNote());
            
            // 🔧 수정: 메모 업데이트 후 엔티티를 명시적으로 저장
            summaryArchiveRepository.save(archive);
            log.info("Archive saved successfully");

            // 활동 로그 저장
            UserActivityLog logEntry = UserActivityLog.builder()
                    .user(archive.getUser())
                    .activityType("USER_NOTE_UPDATED")
                    .targetEntityType("SUMMARY_ARCHIVE")
                    .targetEntityIdInt(archive.getId())
                    .activityDetail("사용자 메모 수정 완료")
                    .details(String.format("{\"summaryTitle\": \"%s\", \"newNote\": \"%s\"}",
                            archive.getSummary().getAudioTranscript().getVideo().getTitle(),
                            requestDTO.getNote()))
                    .createdAt(LocalDateTime.now())
                    .build();
            userActivityLogRepository.save(logEntry);
            log.info("Activity log saved successfully");
            
        } catch (Exception e) {
            log.error("Error updating user notes: ", e);
            throw e; // 예외를 다시 던져서 컨트롤러에서 처리하도록 함
        }
    }

    // 태그 찾기 또는 생성 (동시성 문제 해결)
    private synchronized Tag findOrCreateTag(String tagName) {
        return tagRepository.findByTagName(tagName)
                .orElseGet(() -> tagRepository.save(Tag.builder().tagName(tagName).build()));
    }
}
