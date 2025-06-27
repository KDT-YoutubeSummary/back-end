package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.response.SummaryResponseDTO;
import com.kdt.yts.YouSumback.model.entity.Summary;
import com.kdt.yts.YouSumback.model.entity.SummaryArchive;
import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.repository.SummaryArchiveRepository;
import com.kdt.yts.YouSumback.repository.SummaryRepository;
import com.kdt.yts.YouSumback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryArchiveService {

    private final SummaryArchiveRepository summaryArchiveRepository;
    private final UserRepository userRepository;
    private final SummaryRepository summaryRepository;

    @Transactional
    public void addSummaryToArchive(Long userId, Long summaryId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));

        Summary summary = summaryRepository.findById(summaryId)
                .orElseThrow(() -> new RuntimeException("요약을 찾을 수 없습니다."));

        Optional<SummaryArchive> existingArchive = summaryArchiveRepository.findByUserIdAndSummaryId(userId, summaryId);

        if (existingArchive.isEmpty()) {
            SummaryArchive newArchive = new SummaryArchive();
            newArchive.setUser(user);
            newArchive.setSummary(summary);
            newArchive.setLastViewedAt(LocalDateTime.now());
            summaryArchiveRepository.save(newArchive);
        } else {
            SummaryArchive archive = existingArchive.get();
            archive.setLastViewedAt(LocalDateTime.now());
            summaryArchiveRepository.save(archive);
        }
    }

    @Transactional
    public List<SummaryResponseDTO> getUserSummaryArchives(long userId) {
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        List<SummaryArchive> archives = summaryArchiveRepository.findByUserId(userId);

        return archives.stream()
                .map(archive -> new SummaryResponseDTO(
                        archive.getSummary().getId(),
                        archive.getSummary().getAudioTranscript().getId(),
                        archive.getSummary().getAudioTranscript().getVideo().getId(),
                        archive.getSummary().getSummaryText(),
                        null, // 해시태그 정보는 필요 시 추가 구현
                        archive.getSummary().getAudioTranscript().getVideo().getTitle(),
                        archive.getSummary().getAudioTranscript().getVideo().getThumbnailUrl(),
                        archive.getSummary().getAudioTranscript().getVideo().getUploaderName(),
                        archive.getSummary().getAudioTranscript().getVideo().getViewCount(),
                        archive.getSummary().getAudioTranscript().getVideo().getOriginalLanguageCode(),
                        archive.getSummary().getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
