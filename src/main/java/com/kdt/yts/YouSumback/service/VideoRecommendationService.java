package com.kdt.yts.YouSumback.service;

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
import com.kdt.yts.YouSumback.service.client.YouTubeClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    @Autowired
    private YouTubeClient youTubeClient;

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

                    // 1. 태그를 키워드로 사용해서 YouTube API를 통해 영상 10개 가져오기
                    return Mono.fromCallable(() -> {
                        try {
                            String keyword = String.join(" ", tagNames);
                            List<com.google.api.services.youtube.model.Video> youtubeVideos =
                                youTubeClient.searchVideosByKeyword(keyword, 10);

                            if (youtubeVideos.isEmpty()) {
                                throw new IllegalArgumentException("태그에 맞는 YouTube 영상을 찾을 수 없습니다.");
                            }

                            // YouTube API로 가져온 영상을 Video 엔티티로 변환
                            List<Video> videos = youtubeVideos.stream()
                                    .map(ytVideo -> {
                                        try {
                                            return Video.builder()
                                                .youtubeId(ytVideo.getId())
                                                .title(ytVideo.getSnippet().getTitle())
                                                .originalUrl("https://www.youtube.com/watch?v=" + ytVideo.getId())
                                                .uploaderName(ytVideo.getSnippet().getChannelTitle())
                                                .thumbnailUrl(ytVideo.getSnippet().getThumbnails().getDefault().getUrl())
                                                .viewCount(ytVideo.getStatistics().getViewCount().longValue())
                                                .publishedAt(LocalDateTime.parse(
                                                    ytVideo.getSnippet().getPublishedAt().toStringRfc3339(),
                                                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                                                ))
                                                .build();
                                        } catch (Exception e) {
                                            System.err.println("영상 정보 변환 중 오류: " + e.getMessage());
                                            return null;
                                        }
                                    })
                                    .filter(video -> video != null)
                                    .collect(Collectors.toList());

                            return videos;
                        } catch (Exception e) {
                            System.err.println("YouTube API 호출 중 오류: " + e.getMessage());
                            throw new RuntimeException("YouTube API 호출 실패: " + e.getMessage());
                        }
                    })
                    .flatMap(videos -> {
                        // 2. AI를 이용해 5개 영상 선정 및 추천 이유 생성
                        StringBuilder videoInfo = new StringBuilder();
                        for (Video video : videos) {
                            videoInfo.append("제목: ").append(video.getTitle())
                                    .append(", 채널: ").append(video.getUploaderName())
                                    .append(", URL: ").append(video.getOriginalUrl())
                                    .append("\n");
                        }

                        String prompt = "다음은 태그 [" + String.join(", ", tagNames) + "]에 관련된 10개의 유튜브 영상입니다. " +
                                "이 중에서 태그에 가장 적합하고 유익한 5개의 영상을 선택하고, 각 영상에 대해 시청자에게 적합한 추천 이유를 작성해주세요. " +
                                "반드시 JSON 배열 형식으로 응답해주세요. 형식: [{\"title\":\"영상 제목\", \"url\":\"영상 링크\", \"reason\":\"추천 사유\"}, ...]. " +
                                "영상 목록:\n" + videoInfo.toString();

                        return openAIClient.chat(prompt)
                                .flatMap(response -> {
                                    try {
                                        String cleaned = response.replaceAll("(?s)```json|```|`", "").trim();
                                        ObjectMapper mapper = new ObjectMapper();
                                        List<VideoAiRecommendationResponse> recommendations = mapper.readValue(
                                                cleaned,
                                                mapper.getTypeFactory().constructCollectionType(List.class, VideoAiRecommendationResponse.class)
                                        );

                                        // 최대 5개로 제한
                                        if (recommendations.size() > 5) {
                                            recommendations = recommendations.subList(0, 5);
                                        }

                                        return Mono.just(recommendations);
                                    } catch (Exception e) {
                                        return Mono.error(new RuntimeException("OpenAI 응답이 올바른 JSON 배열 형식이 아닙니다: " + response));
                                    }
                                });
                    });
                });
    }

    // AI 추천 결과를 video_recommendation 테이블에 저장
    @Transactional
    public List<VideoRecommendation> saveAiRecommendation(Long userLibraryId, List<VideoAiRecommendationResponse> responses) {
        UserLibrary userLibrary = userLibraryRepository.findById(userLibraryId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("해당 userLibraryId의 UserLibrary가 없습니다."));
        Video recommendedVideo = userLibrary.getSummary().getAudioTranscript().getVideo();
        List<VideoRecommendation> savedList = new ArrayList<>();

        for (VideoAiRecommendationResponse response : responses) {
            String url = response.getUrl();
            String youtubeId;

            try {
                youtubeId = youtubeMetadataService.extractYoutubeId(url);
            } catch (Exception e) {
                System.err.println("유효하지 않은 유튜브 URL: " + url);
                continue;
            }

            // 비디오가 DB에 있는지 확인
            Optional<Video> videoOpt = videoRepository.findByYoutubeId(youtubeId);
            Video video;

            if (videoOpt.isEmpty()) {
                // DB에 없으면 새로 저장
                try {
                    // YouTubeMetadataService의 기존 메서드 활용
                    youtubeMetadataService.saveVideoMetadata(youtubeId);
                    videoOpt = videoRepository.findByYoutubeId(youtubeId);
                    if (videoOpt.isEmpty()) {
                        System.err.println("영상 저장 후에도 DB에서 찾을 수 없습니다: " + youtubeId);
                        continue;
                    }
                    video = videoOpt.get();
                } catch (Exception e) {
                    System.err.println("영상 메타데이터 저장 실패(" + url + "): " + e.getMessage());
                    continue;
                }
            } else {
                video = videoOpt.get();
            }

            // 추천 정보 저장
            VideoRecommendation recommendation = new VideoRecommendation();
            recommendation.setUser(userLibrary.getUser());
            recommendation.setVideo(video);
            recommendation.setVideo2(recommendedVideo); // 추천의 계기가 된 영상
            recommendation.setRecommendationReason(response.getReason());
            recommendation.setRecommendationAiVersion("GPT-4"); // AI 버전 정보

            VideoRecommendation saved = videoRecommendationRepository.save(recommendation);
            savedList.add(saved);
        }

        return savedList;
    }
}

