package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.config.OpenAIConfig;
import com.kdt.yts.YouSumback.model.entity.VideoRecommendation;
import com.kdt.yts.YouSumback.repository.UserLibraryTagRepository;
import com.kdt.yts.YouSumback.repository.VideoRecommendationRepository;
import com.kdt.yts.YouSumback.service.client.OpenAIClient;
import com.kdt.yts.YouSumback.model.dto.response.VideoAiRecommendationResponse;
import com.kdt.yts.YouSumback.model.entity.UserLibraryTag;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VideoRecommendationService {
    @Autowired
    private VideoRecommendationRepository videoRecommendationRepository;
    @Autowired
    private UserLibraryTagRepository userLibraryTagRepository;
    @Autowired
    private OpenAIClient openAIClient;

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
    public Mono<VideoAiRecommendationResponse> getAiRecommendationByUserLibraryId(Long userLibraryId) {
        return Mono.fromCallable(() -> userLibraryTagRepository.findByUserLibrary_UserLibraryId(userLibraryId))
                .map(tags -> tags.stream().map(t -> t.getTag().getTagName()).toList())
                .flatMap(tagNames -> {
                    if (tagNames.isEmpty()) {
                        return Mono.error(new IllegalArgumentException("해당 라이브러리에 태그가 없습니다."));
                    }
                    String prompt = "아래 태그들을 바탕으로 유튜브 영상을 1개 추천해줘. 반드시 JSON만 응답해. 예시: {\"title\":\"영상 제목\", \"url\":\"영상 링크\", \"reason\":\"추천 사유\"}. 태그: " + tagNames;
                    return openAIClient.chat(prompt)
                            .flatMap(response -> {
                                try {
                                    // 응답에서 백틱 및 json 태그 제거
                                    String cleaned = response.replaceAll("(?s)```json|```|`", "").trim();
                                    ObjectMapper mapper = new ObjectMapper();
                                    return Mono.just(mapper.readValue(cleaned, VideoAiRecommendationResponse.class));
                                } catch (Exception e) {
                                    return Mono.error(new RuntimeException("OpenAI 응답이 올바른 JSON 형식이 아닙니다: " + response));
                                }
                            });
                });
    }

    // AI 추천 결과를 video_recommendation 테이블에 저장 (구현 X)
    public void saveAiRecommendation(Long userLibraryId, VideoAiRecommendationResponse response) {
        // TODO: 구현 예정
    }
}
