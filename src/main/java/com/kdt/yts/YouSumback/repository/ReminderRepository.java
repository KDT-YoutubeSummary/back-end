package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime; // 날짜와 시간 정보
import java.util.List; // 리스트 컬렉션


@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUserId(Long userId);
    List<Reminder> findByIsActiveTrueAndNextNotificationDatetimeLessThanEqual(LocalDateTime localDateTime);
    
    // 특정 요약 저장소의 리마인더 목록 조회
    List<Reminder> findBySummaryArchiveId(Long summaryArchiveId);
    
    // 사용자와 요약 저장소별 리마인더 조회
    List<Reminder> findByUserIdAndSummaryArchiveId(Long userId, Long summaryArchiveId);
}