package com.YouSumback.repository;

import com.YouSumback.model.entity.VideoRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRecommendationRepository extends JpaRepository<VideoRecommendation, Long> {
}
