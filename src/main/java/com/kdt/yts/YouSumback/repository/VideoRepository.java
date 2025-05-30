package com.kdt.yts.YouSumback.repository;

import com.kdt.yts.YouSumback.model.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, String> {
    Optional<Video> findByYoutubeId(String youtubeId);
}
