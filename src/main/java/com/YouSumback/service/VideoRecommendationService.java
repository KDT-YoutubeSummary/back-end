package com.YouSumback.service;

import com.YouSumback.config.OpenAIConfig;
import com.YouSumback.model.entity.User;
import com.YouSumback.model.entity.VideoRecommendation;
import com.YouSumback.repository.UserLibraryTagRepository;
import com.YouSumback.repository.VideoRecommendationRepository;
import com.YouSumback.model.entity.UserLibrary;
import com.YouSumback.repository.UserLibraryRepository;
import com.YouSumback.service.client.OpenAIClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class VideoRecommendationService {
    @Autowired
    private VideoRecommendationRepository videoRecommendationRepository;
    @Autowired
    private UserLibraryRepository userLibraryRepository;
    @Autowired
    private OpenAIClient openAIClient;
    @Autowired
    private UserLibraryTagRepository userLibraryTagRepository;
    @Autowired
    private OpenAIConfig openAIConfig;

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

    /**
     * 라이브러리 식별자를 입력받아 해당 라이브러리에 연결된 해시태그를 바탕으로
     * 관련이 있는 유튜브 영상을 AI에게서 받아오는 함수
     *
     * @param userLibraryId 라이브러리 식별자
     * @return AI가 추천한 유튜브 영상 정보 리스트
     */
    public Mono<List<VideoRecommendation>> getRecommendedVideosByLibraryId(Long userLibraryId) {
        // 라이브러리에 연결된 태그 목록 조회
        var tagList = userLibraryTagRepository.findByUserLibrary_UserLibraryId(userLibraryId);
        if (tagList == null || tagList.isEmpty()) {
            return Mono.just(List.of());
        }

        // 태그 이름 추출
        var tagNames = tagList.stream()
                .map(ut -> ut.getTag().getTagName())
                .toList();

        // 프롬프트 생성 - 구조화된 응답을 요청
        String prompt = "다음 해시태그와 관련된 유튜브 영상을 5개 추천해주세요: " + String.join(", ", tagNames) + "\n\n" +
                "다음 JSON 형식으로 응답해주세요. 각 영상에는 제목(title), 추천 이유(reason), 예상 URL(url)을 포함해주세요:\n" +
                "추천 이유(reason)는 각 해시태그와 영상의 연관성을 구체적으로 설명하고, 단순히 '관련 영상입니다'와 같은 모호한 답변은 피해주세요.\n" +
                "[\n" +
                "  {\n" +
                "    \"title\": \"영상 제목\",\n" +
                "    \"reason\": \"이 해시태그와 관련하여 이 영상을 추천하는 이유\",\n" +
                "    \"url\": \"https://www.youtube.com/watch?v=예상되는영상ID\"\n" +
                "  },\n" +
                "  ...\n" +
                "]\n" +
                "JSON 형식만 응답해주세요.";

        // AI에게 프롬프트 전송 및 결과를 파싱하여 VideoRecommendation 리스트로 변환
        return openAIClient.chat(prompt)
                .flatMap(this::parseAIResponseToVideoRecommendations);
    }

    /**
     * AI 응답을 파싱하여 VideoRecommendation 객체 리스트로 변환
     * @param aiResponse AI의 JSON 형식 응답
     * @return VideoRecommendation 객체 리스트
     */
    private Mono<List<VideoRecommendation>> parseAIResponseToVideoRecommendations(String aiResponse) {
        try {
            // JSON 응답 추출 - AI가 반환한 문자열에서 JSON 부분만 추출
            String jsonResponse = extractJsonFromResponse(aiResponse);

            // JSON 파싱 및 VideoRecommendation 객체 생성
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonArray = objectMapper.readTree(jsonResponse);

            List<VideoRecommendation> recommendations = new java.util.ArrayList<>();

            for (int i = 0; i < jsonArray.size(); i++) {
                com.fasterxml.jackson.databind.JsonNode videoNode = jsonArray.get(i);

                VideoRecommendation recommendation = new VideoRecommendation();
                // 기본 정보 설정
                recommendation.setRecommendationAiVersion("OpenAI-" + openAIConfig.getModel());
                recommendation.setCreateAt(java.time.LocalDateTime.now());
                recommendation.setClicked(false);
                recommendation.setClickedAt(null);

                // AI 응답에서 추출한 정보 설정
                String title = videoNode.get("title").asText();
                String reason = videoNode.get("reason").asText();
                String url = videoNode.get("url").asText();

                // Video 객체를 직접 생성하는 대신 추천 이유에 정보 저장
                StringBuilder recommendationReason = new StringBuilder();
                recommendationReason.append("제목: ").append(title).append("\n");
                recommendationReason.append("추천 이유: ").append(reason).append("\n");
                recommendationReason.append("URL: ").append(url);

                recommendation.setRecommendationReason(recommendationReason.toString());

                recommendations.add(recommendation);
            }

            return Mono.just(recommendations);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("AI 응답 파싱 중 오류 발생: " + e.getMessage(), e));
        }
    }

    /**
     * AI 응답에서 JSON 부분만 추출
     * @param aiResponse AI의 전체 응답
     * @return JSON 문자열
     */
    private String extractJsonFromResponse(String aiResponse) {
        // JSON 배열 시작([)과 끝(]) 찾기
        int startIndex = aiResponse.indexOf('[');
        int endIndex = aiResponse.lastIndexOf(']') + 1;

        if (startIndex == -1 || endIndex == 0 || endIndex <= startIndex) {
            throw new RuntimeException("AI 응답에서 유효한 JSON을 찾을 수 없습니다: " + aiResponse);
        }

        return aiResponse.substring(startIndex, endIndex);
    }

    /**
     * 라이브러리 ID로 해당 라이브러리 소유자의 유저 ID 조회
     * @param userLibraryId 라이브러리 ID
     * @return 라이브러리 소유자의 유저 ID
     */
    public Long getUserIdByLibraryId(Long userLibraryId) {
        UserLibrary userLibrary = userLibraryRepository.findById(userLibraryId)
                .orElseThrow(() -> new RuntimeException("라이브러리를 찾을 수 없습니다: " + userLibraryId));
        return Long.valueOf(userLibrary.getUser().getId());
    }

    /**
     * 라이브러리 ID로 해당 라이브러리 소유자인 User 객체 조회
     * @param userLibraryId 라이브러리 ID
     * @return 라이브러리 소유자의 User 객체
     */
    public User getUserByLibraryId(Long userLibraryId) {
        UserLibrary userLibrary = userLibraryRepository.findById(userLibraryId)
                .orElseThrow(() -> new RuntimeException("라이브러리를 찾을 수 없습니다: " + userLibraryId));
        return userLibrary.getUser();
    }
}

