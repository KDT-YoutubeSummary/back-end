package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLibraryRepository extends JpaRepository<UserLibrary, Integer> {
    // User 엔티티의 userId가 Integer, Summary 엔티티의 summaryId도 Integer이므로
    Optional<UserLibrary> findByUserUserIdAndSummarySummaryId(Integer userId, Integer summaryId);
}



