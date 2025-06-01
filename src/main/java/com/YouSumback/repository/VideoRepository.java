package com.YouSumback.repository;

import com.YouSumback.model.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, String> {
    // video_id가 String 타입임 (YouTube ID)
}
