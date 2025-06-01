package com.YouSumback.service;

import com.YouSumback.model.dto.request.VideoRegisterRequestDto;
import com.YouSumback.model.dto.response.VideoResponseDto;
import com.YouSumback.model.entity.User;
import com.YouSumback.model.entity.Video;
import com.YouSumback.repository.UserRepository;
import com.YouSumback.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class VideoService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    public VideoResponseDto registerVideo(VideoRegisterRequestDto requestDto) {


        // 1. userId로 사용자 조회
        Optional<User> userOptional = userRepository.findByUsername(requestDto.getUsername());
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("해당 사용자(username: " + requestDto.getUsername() + ")가 존재하지 않습니다.");
        }

        User user = userOptional.get();

        // 2. video 생성 및 저장
        Video video = new Video();
        video.setId(UUID.randomUUID().toString()); // UUID로 video_id 생성
        video.setUser(user);
        video.setTitle(requestDto.getTitle());
        video.setVideoUrl(requestDto.getVideoUrl());
        video.setCreatedAt(LocalDateTime.now());

        videoRepository.save(video);

        // 3. 응답 DTO 구성
        return new VideoResponseDto(
                video.getId(),
                (long) user.getId(),
                video.getTitle(),
                video.getVideoUrl()
        );
    }
}
