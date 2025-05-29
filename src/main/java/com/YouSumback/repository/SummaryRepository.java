package com.YouSumback.repository;

import com.YouSumback.model.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SummaryRepository extends JpaRepository<Summary, Integer> {
}
