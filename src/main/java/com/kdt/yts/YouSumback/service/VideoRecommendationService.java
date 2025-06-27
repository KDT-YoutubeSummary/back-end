package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.response.VideoAiRecommendationResponseDTO;
import com.kdt.yts.YouSumback.model.entity.SummaryArchive;
import com.kdt.yts.YouSumback.model.entity.SummaryArchiveTag;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.model.entity.VideoRecommendation;
import com.kdt.yts.YouSumback.repository.*;
import com.kdt.yts.YouSumback.service.client.OpenAIClient;
import com.kdt.yts.YouSumback.service.client.YouTubeClient;
import com.kdt.yts.YouSumback.util.MetadataHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class VideoRecommendationService {

    @Autowired private VideoRecommendationRepository videoRecommendationRepository;
    @Autowired private SummaryArchiveTagRepository summaryArchiveTagRepository;
    @Autowired private OpenAIClient openAIClient;
    @Autowired private VideoRepository videoRepository;
    @Autowired private SummaryArchiveRepository summaryArchiveRepository;
    @Autowired private YouTubeMetadataService youtubeMetadataService;
    @Autowired private YouTubeClient youTubeClient;
    @Autowired private TagRepository tagRepository;
    @Autowired private MetadataHelper metadataHelper;

    public VideoRecommendation createRecommendation(VideoRecommendation videoRecommendation) {
        return videoRecommendationRepository.save(videoRecommendation);
    }

    public List<VideoRecommendation> getRecommendationsByUserId(Long userId) {
        return videoRecommendationRepository.findByUser_Id(userId);
    }

    public void deleteRecommendation(Long id) {
        videoRecommendationRepository.deleteById(id);
    }

    public Mono<List<VideoAiRecommendationResponseDTO>> getAiRecommendationBySummaryArchiveId(Long summaryArchiveId) {
        log.info("AI 추천 시작 - summaryArchiveId: {}", summaryArchiveId);

        return Mono.fromCallable(() -> {
                    log.info("태그 조회 시작 - summaryArchiveId: {}", summaryArchiveId);
                    List<SummaryArchiveTag> tags = summaryArchiveTagRepository.findBySummaryArchiveId(summaryArchiveId);
                    log.info("찾은 태그 개수: {}", tags.size());
                    return tags;
                })
                .map(tags -> tags.stream().map(t -> t.getTag().getTagName()).toList())
                .flatMap(tagNames -> {
                    List<String> finalTagNames = tagNames.isEmpty() ? List.of("교육", "학습", "프로그래밍") : tagNames;
                    log.info("최종 사용할 키워드: {}", finalTagNames);

                    return Mono.fromCallable(() -> {
                                try {
                                    String keyword = String.join(" ", finalTagNames);
                                    log.info("YouTube API 검색 시작 - 키워드: {}", keyword);

                                    List<com.google.api.services.youtube.model.Video> youtubeVideos =
                                            youTubeClient.searchVideosByKeyword(keyword, 10);

                                    log.info("YouTube API 검색 완료 - 결과 개수: {}", youtubeVideos.size());
                                    if (youtubeVideos.isEmpty()) {
                                        throw new IllegalArgumentException("태그에 맞는 YouTube 영상을 찾을 수 없습니다.");
                                    }

                                    return youtubeVideos.stream().map(ytVideo -> {
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
                                                            DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                                    .build();
                                        } catch (Exception e) {
                                            log.error("영상 정보 변환 중 오류: {}", e.getMessage());
                                            return null;
                                        }
                                    }).filter(Objects::nonNull).collect(Collectors.toList());

                                } catch (Exception e) {
                                    log.error("YouTube API 호출 중 오류: {}", e.getMessage(), e);
                                    throw new RuntimeException("YouTube API 호출 실패: " + e.getMessage());
                                }
                            })
                            .flatMap(videos -> {
                                log.info("OpenAI 요청 시작 - 영상 개수: {}", videos.size());

                                StringBuilder videoInfo = new StringBuilder();
                                for (Video video : videos) {
                                    videoInfo.append("제목: ").append(video.getTitle())
                                            .append(", 채널: ").append(video.getUploaderName())
                                            .append(", URL: ").append(video.getOriginalUrl()).append("\n");
                                }

                                String prompt = "다음은 태그 [" + String.join(", ", finalTagNames) + "]에 관련된 " + videos.size() + "개의 유튜브 영상입니다. " +
                                        "이 중에서 태그에 가장 적합하고 유익한 5개의 영상을 선택하고, 각 영상에 대해 시청자에게 적합한 추천 이유를 작성해주세요. " +
                                        "반드시 JSON 배열 형식으로 응답해주세요. 형식: [{\"title\":\"영상 제목\", \"url\":\"영상 링크\", \"reason\":\"추천 사유\"}, ...]. " +
                                        "영상 목록:\n" + videoInfo;

                                return openAIClient.chat(prompt)
                                        .flatMap(response -> {
                                            try {
                                                log.info("OpenAI 응답 받음 - 길이: {}", response.length());
                                                String cleaned = response.replaceAll("(?s)```json|```|`", "").trim();
                                                ObjectMapper mapper = new ObjectMapper();
                                                List<VideoAiRecommendationResponseDTO> recommendations = mapper.readValue(
                                                        cleaned,
                                                        mapper.getTypeFactory().constructCollectionType(List.class, VideoAiRecommendationResponseDTO.class));

                                                log.info("JSON 파싱 완료 - 추천 개수: {}", recommendations.size());
                                                return Mono.just(recommendations.size() > 5 ? recommendations.subList(0, 5) : recommendations);
                                            } catch (Exception e) {
                                                log.error("OpenAI 응답 파싱 실패 - 응답: {}", response, e);
                                                return Mono.error(new RuntimeException("OpenAI 응답이 올바른 JSON 배열 형식이 아닙니다: " + response));
                                            }
                                        });
                            });
                })
                .doOnError(error -> log.error("AI 추천 실패 - summaryArchiveId: {}", summaryArchiveId, error));
    }

    @Transactional
    public List<VideoRecommendation> saveAiRecommendation(Long summaryArchiveId, List<VideoAiRecommendationResponseDTO> responses) {
        SummaryArchive summaryArchive = summaryArchiveRepository.findById(summaryArchiveId)
                .orElseThrow(() -> new IllegalArgumentException("해당 summaryArchiveId의 SummaryArchive가 없습니다."));

        List<VideoRecommendation> recommendations = new ArrayList<>();
        Video originalVideo = summaryArchive.getSummary().getAudioTranscript().getVideo();

        for (VideoAiRecommendationResponseDTO response : responses) {
            String url = response.getUrl();
            String youtubeId;
            try {
                youtubeId = metadataHelper.extractYoutubeId(url);
            } catch (Exception e) {
                log.warn("유효하지 않은 유튜브 URL: {}", url);
                continue;
            }

            Optional<Video> videoOpt = videoRepository.findByYoutubeId(youtubeId);
            Video video;
            if (videoOpt.isEmpty()) {
                try {
                    youtubeMetadataService.saveVideoMetadata(youtubeId);
                    video = videoRepository.findByYoutubeId(youtubeId).orElseThrow();
                } catch (Exception e) {
                    log.warn("영상 메타데이터 저장 실패({}): {}", url, e.getMessage());
                    continue;
                }
            } else {
                video = videoOpt.get();
            }

            VideoRecommendation recommendation = new VideoRecommendation();
            recommendation.setUser(summaryArchive.getUser());
            recommendation.setSourceVideo(originalVideo);
            recommendation.setRecommendedVideo(video);
            recommendation.setRecommendationReason(response.getReason());

            recommendations.add(videoRecommendationRepository.save(recommendation));
        }

        return recommendations;
    }

    public List<VideoAiRecommendationResponseDTO> toResponseDTO(List<VideoRecommendation> recommendations) {
        return recommendations.stream().map(rec -> new VideoAiRecommendationResponseDTO(
                rec.getRecommendedVideo().getTitle(),
                "https://www.youtube.com/watch?v=" + rec.getRecommendedVideo().getYoutubeId(),
                rec.getRecommendationReason()
        )).toList();
    }
}
