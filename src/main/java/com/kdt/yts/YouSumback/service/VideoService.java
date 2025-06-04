package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.request.VideoRegisterRequestDto;
import com.kdt.yts.YouSumback.model.dto.response.VideoRegisterResponseDto;
import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.UserRepository;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final YouTubeMetadataService youTubeMetadataService;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    public VideoRegisterResponseDto saveVideo(VideoRegisterRequestDto dto) throws Exception {
        // 1. 유저가 없으면 생성
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username(dto.getUsername(
