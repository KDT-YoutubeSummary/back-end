package com.YouSumback.repository;

import com.YouSumback.model.entity.VideoRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoRecommendationRepository extends JpaRepository<VideoRecommendation, Long> {
    // 사용자 ID로 영상 추천 목록 찾기
    List<VideoRecommendation> findByUser_Id(Long userId);

    // 추천 영상 삭제
    void deleteById(Long id);
}
