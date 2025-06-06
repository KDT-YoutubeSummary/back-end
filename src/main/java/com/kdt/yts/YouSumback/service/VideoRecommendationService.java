package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.config.OpenAIConfig;
import com.kdt.yts.YouSumback.model.entity.VideoRecommendation;
import com.kdt.yts.YouSumback.repository.UserLibraryTagRepository;
import com.kdt.yts.YouSumback.repository.VideoRecommendationRepository;
import com.kdt.yts.YouSumback.service.client.OpenAIClient;
import com.kdt.yts.YouSumback.model.dto.response.VideoAiRecommendationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import com.kdt.yts.YouSumback.repository.UserLibraryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VideoRecommendationService {
    @Autowired
    private VideoRecommendationRepository videoRecommendationRepository;
    @Autowired
    private UserLibraryTagRepository userLibraryTagRepository;
    @Autowired
    private OpenAIClient openAIClient;
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private UserLibraryRepository userLibraryRepository;
    @Autowired
    private YouTubeMetadataService youtubeMetadataService;

    // 영상 추천 테이블에 등록
    public VideoRecommendation createRecommendation(VideoRecommendation videoRecommendation) {
        return videoRecommendationRepository.save(videoRecommendation);
    }

    // 사용자 ID로 영상 추천 목록 찾기
    public List<VideoRecommendation> getRecommendationsByUserId(Long userId) {
        return videoRecommendationRepository.findByUser_UserId(userId);
    }

    // 영상 추천 삭제
    public void deleteRecommendation(Long id) {
        videoRecommendationRepository.deleteById(id);
    }

    // userLibraryId 기반 AI 추천 (비동기 처리)
    public Mono<List<VideoAiRecommendationResponse>> getAiRecommendationByUserLibraryId(Long userLibraryId) {
        return Mono.fromCallable(() -> userLibraryTagRepository.findByUserLibrary_UserLibraryId(userLibraryId))
                .map(tags -> tags.stream().map(t -> t.getTag().getTagName()).toList())
                .flatMap(tagNames -> {
                    if (tagNames.isEmpty()) {
                        return Mono.error(new IllegalArgumentException("해당 라이브러리에 태그가 없습니다."));
                    }
                    String prompt = "아래 태그들을 바탕으로 유튜브 영상을 5개 추천해줘. 반드시 JSON 배열만 응답해. 예시: [{\"title\":\"영상 제목\", \"url\":\"영상 링크\", \"reason\":\"추천 사유\"}, ...]. 단, 실제로 존재하고 시청 가능한 영상만 추천해줘. 태그: " + tagNames;
                    return openAIClient.chat(prompt)
                            .flatMap(response -> {
                                try {
                                    String cleaned = response.replaceAll("(?s)```json|```|`", "").trim();
                                    ObjectMapper mapper = new ObjectMapper();
                                    List<VideoAiRecommendationResponse> list = mapper.readValue(cleaned, mapper.getTypeFactory().constructCollectionType(List.class, VideoAiRecommendationResponse.class));
                                    return Mono.just(list);
                                } catch (Exception e) {
                                    return Mono.error(new RuntimeException("OpenAI 응답이 올바른 JSON 배열 형식이 아닙니다: " + response));
                                }
                            });
                });
    }

    // AI 추천 결과를 video_recommendation 테이블에 저장 (여러 개 저장, 유효하지 않은 영상은 배제)
    @Transactional
    public List<VideoRecommendation> saveAiRecommendation(Long userLibraryId, List<VideoAiRecommendationResponse> responses) {
        UserLibrary userLibrary = userLibraryRepository.findById(userLibraryId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("해당 userLibraryId의 UserLibrary가 없습니다."));
        Video recommendedVideo = userLibrary.getSummary().getAudioTranscript().getVideo();
        List<VideoRecommendation> savedList = new java.util.ArrayList<>();
        for (VideoAiRecommendationResponse response : responses) {
            String url = response.getUrl();
            String youtubeId;
            try {
                youtubeId = youtubeMetadataService.extractYoutubeId(url);
            } catch (Exception e) {
                System.err.println("유효하지 않은 유튜브 URL: " + url);
                continue;
            }
            try {
                youtubeMetadataService.saveVideoMetadataFromUrl(url);
            } catch (Exception e) {
                // 이미 저장된 영상이면 무시, 그 외는 예외 로그만 남기고 continue
                if (!e.getMessage().contains("이미 저장된 영상")) {
                    System.err.println("영상 메타데이터 저장 실패(" + url + "): " + e.getMessage());
                    continue;
                }
            }
            Optional<Video> videoOpt = videoRepository.findByYoutubeId(youtubeId);
            if (videoOpt.isEmpty()) {
                System.err.println("추천 영상을 Video 테이블에서 찾을 수 없습니다: " + youtubeId);
                continue;
            }
            Video video = videoOpt.get();
            VideoRecommendation recommendation = new VideoRecommendation();
            recommendation.setUser(userLibrary.getUser());
            recommendation.setVideo(video);
            recommendation.setVideo2(recommendedVideo); // recommended_video_id 설정
            recommendation.setRecommendationReason(response.getReason());
            VideoRecommendation saved = videoRecommendationRepository.save(recommendation);
            savedList.add(saved);
        }
        return savedList;
    }
}
