package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.response.SummaryArchiveResponseDTO;
import com.kdt.yts.YouSumback.model.dto.response.TagStatResponseDTO;
import com.kdt.yts.YouSumback.model.entity.*;
import com.kdt.yts.YouSumback.repository.SummaryArchiveRepository;
import com.kdt.yts.YouSumback.repository.SummaryRepository;
import com.kdt.yts.YouSumback.repository.UserRepository;
import com.kdt.yts.YouSumback.repository.TagRepository;
import com.kdt.yts.YouSumback.repository.SummaryArchiveTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryArchiveService {

    private final SummaryArchiveRepository summaryArchiveRepository;
    private final UserRepository userRepository;
    private final SummaryRepository summaryRepository;
    private final TagRepository tagRepository;
    private final SummaryArchiveTagRepository summaryArchiveTagRepository;

    @Transactional
    public void addSummaryToArchive(Long userId, Long summaryId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다. ID: " + userId));
        Summary summary = summaryRepository.findById(summaryId)
                .orElseThrow(() -> new NoSuchElementException("요약을 찾을 수 없습니다. ID: " + summaryId));

        Optional<SummaryArchive> existingArchive = summaryArchiveRepository.findByUserIdAndSummaryId(userId, summaryId);

        if (existingArchive.isPresent()) {
            throw new IllegalStateException("이미 저장된 요약입니다.");
        }

        SummaryArchive newArchive = SummaryArchive.builder()
                .user(user)
                .summary(summary)
                .lastViewedAt(LocalDateTime.now())
                .build();
        summaryArchiveRepository.save(newArchive);
    }

    @Transactional(readOnly = true)
    public List<SummaryArchiveResponseDTO> getUserSummaryArchives(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UsernameNotFoundException("유저를 찾을 수 없습니다. ID: " + userId);
        }
        return summaryArchiveRepository.findByUserId(userId).stream()
                .map(archive -> {
                    List<String> tags = archive.getSummaryArchiveTags().stream()
                            .map(summaryArchiveTag -> summaryArchiveTag.getTag().getTagName())
                            .collect(Collectors.toList());
                    return SummaryArchiveResponseDTO.fromEntity(archive, tags);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SummaryArchiveResponseDTO getArchiveDetail(Long archiveId, Long userId) {
        SummaryArchive archive = summaryArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new NoSuchElementException("ID " + archiveId + "에 해당하는 요약 저장소를 찾을 수 없습니다."));

        if (!archive.getUser().getId().equals(userId)) {
            throw new SecurityException("해당 요약 저장소에 대한 접근 권한이 없습니다.");
        }
        List<String> tags = archive.getSummaryArchiveTags().stream()
                .map(summaryArchiveTag -> summaryArchiveTag.getTag().getTagName())
                .collect(Collectors.toList());
        return SummaryArchiveResponseDTO.fromEntity(archive, tags);
    }

    @Transactional
    public void deleteArchive(Long archiveId, Long userId) {
        SummaryArchive archive = summaryArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new NoSuchElementException("ID " + archiveId + "에 해당하는 요약 저장소를 찾을 수 없습니다."));

        if (!archive.getUser().getId().equals(userId)) {
            throw new SecurityException("해당 요약 저장소에 대한 삭제 권한이 없습니다.");
        }
        summaryArchiveRepository.delete(archive);
    }

    @Transactional(readOnly = true)
    public List<SummaryArchiveResponseDTO> searchArchives(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getUserSummaryArchives(userId);
        }
        return summaryArchiveRepository.searchByUserIdAndKeyword(userId, keyword).stream()
                .map(archive -> {
                    List<String> tags = archive.getSummaryArchiveTags().stream()
                            .map(summaryArchiveTag -> summaryArchiveTag.getTag().getTagName())
                            .collect(Collectors.toList());
                    return SummaryArchiveResponseDTO.fromEntity(archive, tags);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TagStatResponseDTO> getTagStats(Long userId) {
        return summaryArchiveRepository.findTagUsageStatisticsByUserId(userId);
    }

    @Transactional
    public void updateUserNote(Long userId, Long archiveId, String note) {
        SummaryArchive archive = summaryArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new NoSuchElementException("ID " + archiveId + "에 해당하는 요약 저장소를 찾을 수 없습니다."));

        if (!archive.getUser().getId().equals(userId)) {
            throw new SecurityException("해당 요약 저장소에 대한 수정 권한이 없습니다.");
        }
        archive.setUserNotes(note);
        summaryArchiveRepository.save(archive);
    }
}