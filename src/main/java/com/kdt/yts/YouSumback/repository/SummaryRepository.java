package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.Summary;
import org.springframework.stereotype.Repository;

@Repository
public interface SummaryRepository {
    Summary save(Summary testSummary);
}
