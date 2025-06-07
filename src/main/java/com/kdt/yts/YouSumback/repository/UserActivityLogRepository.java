package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.UserActivityLog;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
    List<UserActivityLog> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
}