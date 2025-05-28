package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
    // Define custom query methods if needed
    // For example, find videos by title, uploader, etc.
}
