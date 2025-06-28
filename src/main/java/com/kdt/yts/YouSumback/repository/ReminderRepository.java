package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime; // 날짜와 시간 정보
import java.util.List; // 리스트 컬렉션


@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUser_Id(Long userId);
    
    // ✅ Fetch Join을 사용하여 연관 엔티티를 한 번에 조회
    @Query("SELECT r FROM Reminder r " +
           "JOIN FETCH r.user " +
           "JOIN FETCH r.summaryArchive sa " +
           "JOIN FETCH sa.summary s " +
           "WHERE r.user.id = :userId")
    List<Reminder> findByUserIdWithFetchJoin(@Param("userId") Long userId);
    
    List<Reminder> findByIsActiveTrueAndNextNotificationDatetimeLessThanEqual(LocalDateTime localDateTime);
}