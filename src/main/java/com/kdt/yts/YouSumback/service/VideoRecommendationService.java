package com.kdt.yts.YouSumback.service;

import com.kdt.yts.YouSumback.model.dto.response.VideoAiRecommendationResponseDTO;
import com.kdt.yts.YouSumback.model.entity.VideoRecommendation;
import com.kdt.yts.YouSumback.repository.VideoRecommendationRepository;
import com.kdt.yts.YouSumback.service.client.OpenAIClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.yts.YouSumback.model.entity.Video;
import com.kdt.yts.YouSumback.repository.VideoRepository;
import com.kdt.yts.YouSumback.service.client.YouTubeClient;
import com.kdt.yts.YouSumback.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import com.kdt.yts.YouSumback.repository.SummaryArchiveTagRepository;
import com.kdt.yts.YouSumback.model.entity.SummaryArchive;
import com.kdt.yts.YouSumback.repository.SummaryArchiveRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class VideoRecommendationService {
    @Autowired
    private VideoRecommendationRepository videoRecommendationRepository;
    @Autowired
    private SummaryArchiveTagRepository summaryArchiveTagRepository;
    @Autowired
    private OpenAIClient openAIClient;
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private SummaryArchiveRepository summaryArchiveRepository;
    @Autowired
    private YouTubeMetadataService youtubeMetadataService;
    @Autowired
    private YouTubeClient youTubeClient;
    @Autowired
    private TagRepository tagRepository;

    // 영상 추천 테이블에 등록
    public VideoRecommendation createRecommendation(VideoRecommendation videoRecommendation) {
        return videoRecommendationRepository.save(videoRecommendation);
    }

    // 사용자 ID로 영상 추천 목록 찾기
    public List<VideoRecommendation> getRecommendationsByUserId(Long userId) {
        return videoRecommendationRepository.findByUser_Id(userId);
    }

    // 영상 추천 삭제
    public void deleteRecommendation(Long id) {
        videoRecommendationRepository.deleteById(id);
    }

    // summaryArchiveId 기반 AI 추천 (비동기 처리)
    public Mono<List<VideoAiRecommendationResponseDTO>> getAiRecommendationBySummaryArchiveId(Long summaryArchiveId) {
        log.info("AI 추천 시작 - summaryArchiveId: {}", summaryArchiveId);
        
        return Mono.fromCallable(() -> {
            log.info("태그 조회 시작 - summaryArchiveId: {}", summaryArchiveId);
            List<com.kdt.yts.YouSumback.model.entity.SummaryArchiveTag> tags = summaryArchiveTagRepository.findBySummaryArchiveId(summaryArchiveId);
            log.info("찾은 태그 개수: {}", tags.size());
            return tags;
        })
        .map(tags -> {
            List<String> tagNames = tags.stream().map(t -> t.getTag().getTagName()).toList();
            log.info("태그 이름들: {}", tagNames);
            return tagNames;
        })
        .flatMap(tagNames -> {
            // 태그가 없으면 기본 키워드 사용
            final List<String> finalTagNames;
            if (tagNames.isEmpty()) {
                log.warn("summaryArchiveId {}에 태그가 없습니다. 기본 키워드를 사용합니다.", summaryArchiveId);
                finalTagNames = List.of("교육", "학습", "프로그래밍"); // 기본 키워드
            } else {
                finalTagNames = tagNames;
            }
            
            log.info("최종 사용할 키워드: {}", finalTagNames);

            // 1. 태그를 키워드로 사용해서 YouTube API를 통해 영상 10개 가져오기
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
                                    log.error("영상 정보 변환 중 오류: {}", e.getMessage());
                                    return null;
                                }
                            })
                            .filter(video -> video != null)
                            .collect(Collectors.toList());

                    log.info("비디오 엔티티 변환 완료 - 변환된 개수: {}", videos.size());
                    return videos;
                } catch (Exception e) {
                    log.error("YouTube API 호출 중 오류: {}", e.getMessage(), e);
                    throw new RuntimeException("YouTube API 호출 실패: " + e.getMessage());
                }
            })
            .flatMap(videos -> {
                log.info("OpenAI 요청 시작 - 영상 개수: {}", videos.size());
                
                // 2. AI를 이용해 5개 영상 선정 및 추천 이유 생성  
                StringBuilder videoInfo = new StringBuilder();
                for (Video video : videos) {
                    videoInfo.append("제목: ").append(video.getTitle())
                            .append(", 채널: ").append(video.getUploaderName())
                            .append(", URL: ").append(video.getOriginalUrl())
                            .append("\n");
                }

                String prompt = "다음은 태그 [" + String.join(", ", finalTagNames) + "]에 관련된 " + videos.size() + "개의 유튜브 영상입니다. " +
                        "이 중에서 태그에 가장 적합하고 유익한 5개의 영상을 선택하고, 각 영상에 대해 시청자에게 적합한 추천 이유를 작성해주세요. " +
                        "반드시 JSON 배열 형식으로 응답해주세요. 형식: [{\"title\":\"영상 제목\", \"url\":\"영상 링크\", \"reason\":\"추천 사유\"}, ...]. " +
                        "영상 목록:\n" + videoInfo.toString();

                log.info("OpenAI 프롬프트 길이: {}", prompt.length());

                return openAIClient.chat(prompt)
                        .flatMap(response -> {
                            try {
                                log.info("OpenAI 응답 받음 - 길이: {}", response.length());
                                
                                String cleaned = response.replaceAll("(?s)```json|```|`", "").trim();
                                ObjectMapper mapper = new ObjectMapper();
                                List<VideoAiRecommendationResponseDTO> recommendations = mapper.readValue(
                                        cleaned,
                                        mapper.getTypeFactory().constructCollectionType(List.class, VideoAiRecommendationResponseDTO.class)
                                );

                                log.info("JSON 파싱 완료 - 추천 개수: {}", recommendations.size());

                                // 최대 5개로 제한
                                if (recommendations.size() > 5) {
                                    recommendations = recommendations.subList(0, 5);
                                }

                                return Mono.just(recommendations);
                            } catch (Exception e) {
                                log.error("OpenAI 응답 파싱 실패 - 응답: {}", response, e);
                                return Mono.error(new RuntimeException("OpenAI 응답이 올바른 JSON 배열 형식이 아닙니다: " + response));
                            }
                        });
            });
        })
        .doOnError(error -> log.error("AI 추천 실패 - summaryArchiveId: {}", summaryArchiveId, error));
    }

    // AI 추천 결과를 video_recommendation 테이블에 저장
    @Transactional
    public List<VideoRecommendation> saveAiRecommendation(Long summaryArchiveId, List<VideoAiRecommendationResponseDTO> responses) {
        SummaryArchive summaryArchive = summaryArchiveRepository.findById(summaryArchiveId)
                .orElseThrow(() -> new IllegalArgumentException("해당 summaryArchiveId의 SummaryArchive가 없습니다."));
        //        Video recommendedVideo = summaryArchive.getSummary().getAudioTranscript().getVideo();

        // 추천 영상을 찾거나 새로 생성
        List<VideoRecommendation> recommendations = new ArrayList<>();
        Video OriginalVideo = summaryArchive.getSummary().getAudioTranscript().getVideo();

        for (VideoAiRecommendationResponseDTO response : responses) {
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
            recommendation.setUser(summaryArchive.getUser());
            recommendation.setSourceVideo(OriginalVideo);
            recommendation.setRecommendedVideo(video); // recommended_video_id 설정
//            recommendation.setSourceVideo(video);
//            recommendation.setRecommendedVideo(recommendedVideo); // 추천의 계기가 된 영상
            recommendation.setRecommendationReason(response.getReason());
//            recommendation.setRecommendationAiVersion("GPT-4"); // AI 버전 정보

            VideoRecommendation saved = videoRecommendationRepository.save(recommendation);
            recommendations.add(saved);
        }

        return recommendations;
    }
    public List<VideoAiRecommendationResponseDTO> toResponseDTO(List<VideoRecommendation> recommendations) {
        return recommendations.stream().map(rec -> new VideoAiRecommendationResponseDTO(
                rec.getRecommendedVideo().getTitle(),  // 추천된 영상 제목
                "https://www.youtube.com/watch?v=" + rec.getRecommendedVideo().getYoutubeId(), // 추천된 영상 링크
                rec.getRecommendationReason()  // 추천 사유
        )).toList();
    }
}

